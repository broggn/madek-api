(ns madek.api.resources.admins
  (:require
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]

   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   ;; all needed imports
   [logbug.catcher :as catcher]
   ;[leihs.core.db :as db]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.resources.shared :as sd]

   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]


   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [spy]]))

(defn handle_list-admin
  [req]
  (let [qd (if (true? (-> req :parameters :query :full_data))
             :admins.*
             :admins.id)
        db-result (sd/query-find-all :admins qd)]
    ;(logging/info "handle_list-admin" "\nqd\n" qd "\nresult\n" db-result)
    (sd/response_ok {:admins db-result})))

(defn handle_get-admin
  [req]
  (let [admin (-> req :admin)]
    ;(logging/info "handle_get-admin" admin)
    (sd/response_ok admin)))

(defn handle_create-admin
  [req]
  (catcher/with-logging {}
    (if-let [admin (-> req :admin)]
      ; already has admin
      (sd/response_ok admin)
      ; create admin entry
      ;(let [user (-> req :user)
      ;      id (:id user)
      ;      data {:user_id id}
      ;      ins-res (jdbc/insert! (get-ds) :admins data)]

      ;(let [user (-> req :user)
      ;      id (:id user)
      ;      data {:user_id id}
      ;      sql-map {:insert-into :admins
      ;               :values [data]}
      ;      sql (-> sql-map sql-format)
      ;      ins-res (jdbc/execute! (get-ds) [sql id])]

      (let [user (-> req :user)
            id (:id user)
            data {:user_id id}
            sql (spy (-> (sql/insert-into :admins)
                         (sql/values [data])
                         (sql/returning :*)
                         sql-format))
            ;ins-res (spy (jdbc/execute! (get-ds) [sql])) broken
            ins-res (spy (jdbc/execute! (get-ds) sql))]

        (sd/logwrite req (str "handle_create-admin:" " user-id: " id " result: " ins-res))
        (if-let [result (first ins-res)]
          (sd/response_ok result)
          (sd/response_failed "Could not create admin." 406))))))

(defn handle_delete-admin
  [req]
  (catcher/with-logging {}
    (let [admin (-> req :admin)
          admin-id (:id admin)
          sql (-> (sql/delete-from :admins)
                  (sql/where [:= :id admin-id])
                  (sql/returning :* )
                  sql-format
                  spy)
          del-result (spy (jdbc/execute-one! (get-ds) sql))]

      (if del-result
        (sd/response_ok del-result)
        (sd/response_failed "Could not delete admin." 406)))))

;### wrappers #################################################################

(defn wwrap-find-admin [param colname send404]
  (fn [handler]
    (fn [request] (sd/req-find-data
                    request handler param
                    :admins colname :admin send404))))

(defn wwrap-find-user [param]
  (fn [handler]
    (fn [request] (sd/req-find-data
                    request handler param
                    :users :id :user true))))

;### swagger io schema ########################################################

(def schema_export-admin
  {:id s/Uuid
   ;:user_id s/Uuid
   ;:updated_at s/Any
   ;:created_at s/Any}
   (s/optional-key :user_id) s/Uuid
   (s/optional-key :updated_at) s/Any
   (s/optional-key :created_at) s/Any
   })


;### wrappers #################################################################

; TODO docu
(def ring-routes
  ["/admins"
   {:swagger {:tags ["admin/admins"] :security [{"auth" []}]}}
   ["/"
    {:get
     {:summary (sd/sum_adm (t "List admin users."))
      :handler handle_list-admin
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :full_data) s/Bool}}
      :responses {200 {:body {:admins [schema_export-admin]}}}}}] ;; TODO: fixme
      ;:responses {200 {:body {:admins [s/Any]}}}}}]

   ; edit admin
   ["/admins/:id"
    {:get
     {:summary (sd/sum_adm (t "Get admin by id."))
      :handler handle_get-admin
      :middleware [wrap-authorize-admin!
                   (wwrap-find-admin :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export-admin}
                  404 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm (t "Delete admin by id."))
      :coercion reitit.coercion.schema/coercion
      :handler handle_delete-admin
      :middleware [wrap-authorize-admin!
                   (wwrap-find-admin :id :id true)]
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export-admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]

   ; access via user
   ["/admins/:user_id/user"
    {:post
     {:summary (sd/sum_adm (t "Create admin for user with id."))
      :handler handle_create-admin
      :middleware [wrap-authorize-admin!
                   (wwrap-find-user :user_id)
                   (wwrap-find-admin :user_id :user_id false)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:user_id s/Uuid}}
      :responses {200 {:body schema_export-admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :get
     {:summary (sd/sum_adm (t "Get admin for user."))
      :handler handle_get-admin
      :middleware [wrap-authorize-admin!
                   (wwrap-find-admin :user_id :user_id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:user_id s/Uuid}}
      :responses {200 {:body schema_export-admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm (t "Delete admin for user."))
      :coercion reitit.coercion.schema/coercion
      :handler handle_delete-admin
      :middleware [wrap-authorize-admin!
                   (wwrap-find-admin :user_id :user_id true)]
      :parameters {:path {:user_id s/Uuid}}
      :responses {200 {:body schema_export-admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]])