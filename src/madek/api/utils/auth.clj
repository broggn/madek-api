(ns madek.api.utils.auth
  (:require
   [clj-uuid]
   [clojure.java.jdbc :as jdbco]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   ;[madek.api.utils.rdbms :as rdbms]

   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

         ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

;### admin check ##############################################################

(defn authorize-admin! [request handler]
  "Checks if the authenticated-entity is an admin by either
  checking (-> request :authenticated-entity :is_admin) if present or performing
  an db query.  If so adds {:is_amdin true} to the requests an calls handler.
  Throws a ExceptionInfo with status 403 otherwise. "

  (println "authorize-admin! request.id: " (-> request :authenticated-entity :id))
  (println "authorize-admin! request.is_admin: " (-> request :is_admin))

  (handler
   (or
      ;(if (contains? (-> request :authenticated-entity) :is_admin)
    (if (contains? request :is_admin)
        ;(when (-> request :authenticated-entity :is_admin) request)
      (when (-> request :is_admin) request)
      (when (->> (-> (sql/select [true :is_admin])
                     (sql/from :admins)
                     (sql/where [:= :admins.user_id (-> request :authenticated-entity :id)])
                     sql-format)
                 (jdbc/execute! (get-ds))
                 first :is_admin)
          ;(assoc-in request [:authenticated-entity :is_admin] true)))
        (assoc-in request [:is_admin] true)))
    (throw
     (ex-info
      "Only administrators are allowed to access this resource."
      {:status 403
       :body {:msg "Only administrators are allowed to access this resource."}})))))

(defn wrap-authorize-admin! [handler]
  (fn [req]
    (authorize-admin! req handler)))
