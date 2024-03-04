(ns madek.api.resources.roles.role
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   ;[madek.api.utils.sql :as sql]

   [madek.api.db.core :refer [get-ds]]
   [madek.api.pagination :as pagination]
   [madek.api.resources.shared :as sd]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

(defn export-role [db-role]
  (select-keys db-role [:id :meta_key_id :labels :created_at]))

(defn- transform-ml-role [role]
  (when-let [labels (:labels role)]
    (assoc role :labels (sd/transform_ml (:labels role)))))

(defn- query-index
  [query-params]
  (-> (sql/select :roles.*)
      (sql/from :roles)
      (pagination/add-offset-for-honeysql query-params)
      (sql-format)))

(defn get-index
  [request]
  (let [query-params (-> request :parameters :query)
        dbresult (jdbc/execute! (get-ds) (query-index query-params))
        result (map transform-ml-role dbresult)]
    (sd/response_ok {:roles result})))

(defn query_role-find-one [id]
  (-> (sql/select :*)
      (sql/from :roles)
      (sql/where [:= :roles.id id])
      (sql-format)))

(defn db_role-find-one [id]
  (let [query (query_role-find-one id)
        resultdb (jdbc/execute-one! (get-ds) query)]
    resultdb))

(defn handle_get-role-usr [request]
  (let [id (-> request :parameters :path :id)]
    (if-let [resultdb (db_role-find-one id)]
      (let [result (-> resultdb
                       export-role
                       transform-ml-role)]
        (sd/response_ok result))
      (sd/response_failed "No such role." 404))))

(defn handle_get-role-admin [request]
  (let [id (-> request :parameters :path :id)]
    (if-let [resultdb (db_role-find-one id)]
      (let [result (transform-ml-role resultdb)]
        (sd/response_ok result))
      (sd/response_failed "No such role." 404))))

(defn handle_create-role
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            auth-id (-> req :authenticated-entity :id)
            ins-data (assoc data :creator_id auth-id)
            ;ins-res (jdbc/insert! (rdbms/get-ds) :roles ins-data)] ;TODO

            insert-stmt (-> (sql/insert-into :roles)
                            (sql/values [ins-data])
                            (sql-format))
            ins-res (jdbc/execute-one! (get-ds) [insert-stmt])]

        (logging/info "handle_create-role: " "\new-data:\n" data "\nresult:\n" ins-res)

        (if-let [result (::jdbc/update-count ins-res)]
          (sd/response_ok (transform-ml-role result))
          (sd/response_failed "Could not create role." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-role
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            dwid (assoc data :id id)
            upd-query (sd/sql-update-clause "id" (str id))

;upd-result (jdbc/update! (rdbms/get-ds) :roles dwid upd-query)] ; TODO

            update-stmt (-> (sql/update :roles)

                        ;(sql/set-fields dwid)
                            (sql/set dwid)

                            (sql/where [:= :id id])
                            (sql-format))
            upd-result (jdbc/execute-one! (get-ds) [update-stmt])]

        (logging/info "handle_update-role: " id "\nnew-data\n" dwid "\nupd-result\n" upd-result)

        (if (= 1 (::jdbc/update-count upd-result))
          (sd/response_ok (transform-ml-role
                           (sd/query-eq-find-one :roles :id id)))
          (sd/response_failed "Could not update role." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_delete-role
  [req]
  (try
    (catcher/with-logging {}
      (let [id (-> req :parameters :path :id)]
        (if-let [role (sd/query-eq-find-one :roles :id id)]

          (let [del-query (sd/sql-update-clause "id" id)

;del-result (jdbc/delete! (rdbms/get-ds) :roles del-query)] ;TODO

                delete-stmt (-> (sql/delete-from :roles)
                                (sql/where [:= :id id])
                                (sql-format))
                del-result (jdbc/execute-one! (get-ds) [delete-stmt])]

            (logging/info "handle_delete-role: " id " result: " del-result)
            (if (= 1 (::jdbc/update-count del-result))
              (sd/response_ok (transform-ml-role role))
              (sd/response_failed "Could not delete role." 406)))

          (sd/response_failed "No such role." 404))))
    (catch Exception ex (sd/response_exception ex))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
