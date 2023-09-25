(ns madek.api.authentication
  (:require
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    [madek.api.authentication.basic :as basic-auth]
    [madek.api.authentication.session :as session-auth]
    [madek.api.authentication.token :as token-auth]
    [ring.util.request :as request]
    ))

(defn- add-www-auth-header-if-401 [response]
  (case (:status response)
    401 (assoc-in response [:headers "WWW-Authenticate"]
                  (str "Basic realm=\"Madek ApiClient with password"
                       " or User with token.\""))
    response))

(defn wrap-audit [handler]
  (fn [request]
    (logging/info "wrap auth "
                  " - method: " (:request-method request)
                  " - path: " (request/path-info request)
                  
                  " - auth-method: " (-> request :authentication-method)
                  " - type: " (-> request :authenticated-entity :type)
                  " - is_admin: " (:is_admin request)
                  " - auth-entity: " (-> request :authenticated-entity :id)
                  
                  ;"\n - response status: " (:status response)
                  )
    (handler request)
    ))

(defn wrap [handler]
  (fn [request]
    (let [response ((-> handler
                        session-auth/wrap
                        token-auth/wrap
                        basic-auth/wrap
                        ) request)]
      (add-www-auth-header-if-401 response))))


;### Debug ####################################################################
;(debug/debug-ns *ns*)
