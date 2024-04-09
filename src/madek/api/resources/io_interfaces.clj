(ns madek.api.resources.io-interfaces
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [t]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [error info]]))

;### handlers #################################################################

(defn handle_list-io_interface
  [req]
  (let [full_data (true? (-> req :parameters :query :full_data))
        qd (if (true? full_data) :* :io_interfaces.id)
        db-result (sd/query-find-all :io_interfaces qd)]

    ;(info "handle_list-io_interface" "\nqd\n" qd "\nresult\n" db-result)
    (sd/response_ok db-result)))

(defn handle_get-io_interface
  [req]
  (let [io_interface (-> req :io_interface)]
    ;(info "handle_get-io_interface" io_interface)
    (sd/response_ok io_interface)))

(defn handle_create-io_interfaces
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            sql-query (-> (sql/insert-into :io_interfaces)
                          (sql/values [data])
                          (sql/returning :*)
                          sql-format)
            ins-res (jdbc/execute-one! (get-ds) sql-query)]
        (info "handle_create-io_interfaces: " "\ndata:\n" data "\nresult:\n" ins-res)

        (if-let [result ins-res]
          (sd/response_ok result)
          (sd/response_failed "Could not create io_interface." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-io_interfaces
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            dwid (assoc data :id id)
            sql-query (-> (sql/update :io_interfaces)
                          (sql/set dwid)
                          (sql/where [:= :id id])
                          sql-format)
            upd-result (jdbc/execute-one! (get-ds) sql-query)]

        (info "handle_update-io_interfaces: " "id: " id "\nnew-data:\n" dwid "\nresult: " upd-result)

        (if (= 1 (::jdbc/update-count upd-result))
          (sd/response_ok (sd/query-eq-find-one :io_interfaces :id id))
          (sd/response_failed "Could not update io_interface." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_delete-io_interface
  [req]
  (try
    (catcher/with-logging {}
      (let [io_interface (-> req :io_interface)
            id (-> req :parameters :path :id)
            sql-query (-> (sql/delete-from :io_interfaces)
                          (sql/where [:= :id id])
                          sql-format)
            del-result (jdbc/execute-one! (get-ds) sql-query)]

        (if (= 1 (::jdbc/update-count del-result))
          (sd/response_ok io_interface)
          (error "Could not delete io_interface: " id))))
    (catch Exception e (sd/response_exception e))))

(defn wrap-find-io_interface [handler]
  (fn [request] (sd/req-find-data request handler :id
                                  :io_interfaces
                                  :id :io_interface true)))

;### swagger io schema ########################################################

(def schema_import_io_interfaces
  {:id s/Str
   :description s/Str})

(def schema_update_io_interfaces
  {;(s/optional-key :id) s/Str
   (s/optional-key :description) s/Str})

(def schema_export_io_interfaces_opt
  {:id s/Str
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any})

(def schema_export_io_interfaces
  {:id s/Str
   :description (s/maybe s/Str)
   :created_at s/Any
   :updated_at s/Any})

;### routes ###################################################################
; TODO docu
(def admin-routes
  ["/io_interfaces"
   {:swagger {:tags ["admin/io_interfaces"] :security [{"auth" []}]}}
   ["/"
    {:post
     {:summary (sd/sum_adm (t "Create io_interfaces."))
      :handler handle_create-io_interfaces
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:body schema_import_io_interfaces}
      :responses {200 {:body schema_export_io_interfaces}
                  406 {:body s/Any}}}

     ; io_interface list / query
     :get
     {:summary (sd/sum_adm (t "List io_interfaces."))
      :handler handle_list-io_interface
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :full_data) s/Bool}}
      :responses {200 {:body [schema_export_io_interfaces_opt]}}}}]

   ; edit io_interface
   ["/:id"
    {:get
     {:summary (sd/sum_adm (t "Get io_interfaces by id."))
      :handler handle_get-io_interface
      :middleware [wrap-authorize-admin!
                   wrap-find-io_interface]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Str}}
      :responses {200 {:body schema_export_io_interfaces}
                  404 {:body s/Any}}}

     :put
     {:summary (sd/sum_adm (t "Update io_interfaces with id."))
      :handler handle_update-io_interfaces
      :middleware [wrap-authorize-admin!
                   wrap-find-io_interface]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Str}
                   :body schema_update_io_interfaces}
      :responses {200 {:body schema_export_io_interfaces}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm (t "Delete io_interface by id."))
      :coercion reitit.coercion.schema/coercion
      :handler handle_delete-io_interface
      :middleware [wrap-authorize-admin!
                   wrap-find-io_interface]
      :parameters {:path {:id s/Str}}
      :responses {200 {:body schema_export_io_interfaces}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]])