(ns madek.api.authentication.basic
  (:require
    [camel-snake-kebab.core :refer :all]
    [cider-ci.open-session.bcrypt :refer [checkpw]]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [inflections.core :refer :all]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    [madek.api.authentication.token :as token-authentication]
    [madek.api.utils.rdbms :as rdbms]
    [madek.api.resources.shared :as sd]
    [clojure.walk :refer [keywordize-keys]]
    )
  (:import
    [java.util Base64]
    ))

(defn- get-by-login [table-name login]
  (->> (jdbc/query (rdbms/get-ds)
                   [(str "SELECT * FROM " table-name " WHERE login = ?") login])
       (map #(assoc % :type (-> table-name ->PascalCase singular)))
       (map #(clojure.set/rename-keys % {:email :email_address}))
       first))

(defn- get-api-client-by-login [login]
  (->> (jdbc/query (rdbms/get-ds)
                   [(str "SELECT * FROM api_clients WHERE login = ?") login])
       (map #(assoc % :type "ApiClient"))
       first))

(defn- get-user-by-login-or-email-address [login-or-email]
  (->> (jdbc/query (rdbms/get-ds)
                   [(str "SELECT * FROM users WHERE login = ? OR email = ?")
                    login-or-email login-or-email])
       (map #(assoc % :type "User"))
       (map #(clojure.set/rename-keys % {:email :email_address}))
       first))

(defn get-entity-by-login-or-email [login-or-email]
  (or (get-api-client-by-login login-or-email)
      (get-user-by-login-or-email-address login-or-email)))

(defn- get-auth-systems-user [userId]
  (->> (jdbc/query (rdbms/get-ds)
                   [(str "SELECT * FROM auth_systems_users WHERE auth_system_id = ? AND user_id = ? ") 
                    "password" userId])
       first))

(defn base64-decode [^String encoded]
  (String. (.decode (Base64/getDecoder) encoded)))

(defn extract [request]
  (logging/debug 'extract request)
  (try (when-let [auth-header (-> request :headers keywordize-keys :authorization)]
         (when (re-matches #"(?i)^basic\s+.+$" auth-header)
           (let [decoded-val (base64-decode (last (re-find #"(?i)^basic (.*)$" auth-header)))
                 [username password] (clojure.string/split (str decoded-val) #":" 2)]
             {:username username :password password})))
       (catch Exception _
         (logging/warn "failed to extract basic-auth properties because" _ ))))

(defn user-password-authentication [login-or-email password handler request]
  (if-let [entity (get-entity-by-login-or-email login-or-email)]
    (if-let [asuser (get-auth-systems-user (:id entity))]
      (if-not (checkpw password (:data asuser))
      ;(if-not (checkpw password (:password_digest entity)); if there is an entity the password must match
        {:status 401 :body (str "Password mismatch for "
                                {:login-or-email-address login-or-email})}
        
        (handler (assoc request
                        :authenticated-entity entity
                        ; TODO move into ae
                        :is_admin (sd/is-admin (or (:id entity) (:user_id entity) ))
                        :authentication-method "Basic Authentication")))
            
      {:status 401 :body (str "Only password auth users supported for basic auth.")})
    
    {:status 401 :body (str "Neither User nor ApiClient exists for "
                            {:login-or-email-address login-or-email})}))

(defn authenticate [request handler]
  "Authenticate with the following rules:
  * carry on of there is no auth header with request as is,
  * return 401 if there is a login but we don't find id in DB,
  * return 401 if there is a login and entity but the password doesn't match,
  * return 403 if we find the token but the scope does not suffice,
  * carry on by adding :authenticated-entity to the request."
  (let [{username :username password :password} (extract request)]
    (if-not username
      (handler request); carry on without authenticated entity
      (if-let [user-token (token-authentication/find-user-token-by-some-secret [username password])]
        (token-authentication/authenticate user-token handler request)
        (user-password-authentication username password handler request)))))


(defn wrap [handler]
  (fn [request]
    (authenticate request handler)))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
