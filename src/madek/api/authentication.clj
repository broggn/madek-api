(ns madek.api.authentication
  (:require
   [madek.api.authentication.basic :as basic-auth]
   [madek.api.authentication.session :as session-auth]
   [madek.api.authentication.token :as token-auth]
   [ring.util.request :as request]
   [taoensso.timbre :refer [info]]))

(defn- add-www-auth-header-if-401 [response]
  (case (:status response)
    401 (assoc-in response [:headers "WWW-Authenticate"]
                  (str "Basic realm=\"Madek ApiClient with password"
                       " or User with token.\""))
    response))

;TODO: remove this
(defn set-is-admin-if-present [request]
  (if (nil? (get-in request [:headers "is_admin"]))
    request
    (assoc request :is_admin true)))

;TODO: remove this
(defn set-authenticated-entity-id-if-present [request]
  (let [header-id (get-in request [:headers "id"])]
    (if (nil? header-id)
      request
      (assoc-in request [:authenticated-entity :id] header-id))))

(defn wrap-log [handler]
  (fn [request]
    (let [;; FYI: set is_admin by headers-attr (is_admin)
          request (set-is-admin-if-present request) ;TODO: remove this
          request (set-authenticated-entity-id-if-present request)] ;TODO: remove this

      (info "wrap auth "
            " - method: " (:request-method request)
            " - path: " (request/path-info request)
            " - auth-method: " (-> request :authentication-method)
            " - type: " (-> request :authenticated-entity :type)
            " - is_admin: " (:is_admin request)
            " - auth-entity: " (-> request :authenticated-entity :id))
      (handler request))))

(defn wrap [handler]
  (fn [request]
    (let [response ((-> handler
                        session-auth/wrap
                        token-auth/wrap
                        basic-auth/wrap) request)]
      (add-www-auth-header-if-401 response))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
