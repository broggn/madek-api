(ns madek.api.resources.context-keys
  (:require
   [clojure.java.jdbc :as jdbco]
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sqlo]
   
   [clojure.tools.logging :as logging]

   [logbug.catcher :as catcher]
   [madek.api.pagination :as pagination]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   
         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]
               
               [madek.api.db.core :refer [get-ds]]
   
   [reitit.coercion.schema]
   [schema.core :as s]))

(defn context_key_transform_ml [context_key]
  (assoc context_key
         :hints (sd/transform_ml (:hints context_key))
         :labels (sd/transform_ml (:labels context_key))
         :descriptions (sd/transform_ml (:descriptions context_key))
         :documentation_urls (sd/transform_ml (:documentation_urls context_key))))

(defn handle_adm-list-context_keys
  [req]
  (let [req-query (-> req :parameters :query)
        db-query (-> (sql/select :*)
                     (sql/from :context_keys)

                     (sd/build-query-param req-query :id)
                     (sd/build-query-param req-query :context_id)
                     (sd/build-query-param req-query :meta_key_id)
                     (sd/build-query-param req-query :is_required)

                     (sd/build-query-created-or-updated-after req-query :changed_after)
                     (sd/build-query-ts-after req-query :created_after "created_at")
                     (sd/build-query-ts-after req-query :updated_after "updated_at")

                     (pagination/add-offset-for-honeysql req-query)
                     sql-format)
        db-result (jdbc/execute! (get-ds) db-query)
        tf (map context_key_transform_ml db-result)]

    ;(logging/info "handle_adm-list-context_keys" "\ndb-query\n" db-query)
    (sd/response_ok tf)))

(defn handle_usr-list-context_keys
  [req]
  (let [req-query (-> req :parameters :query)
        db-query (-> (sql/select :id :context_id :meta_key_id
                                 :is_required :position :length_min :length_max
                                 :labels :hints :descriptions :documentation_urls)
                     (sql/from :context_keys)
                     (sd/build-query-param req-query :id)
                     (sd/build-query-param req-query :context_id)
                     (sd/build-query-param req-query :meta_key_id)
                     (sd/build-query-param req-query :is_required)
                     sql-format)
        db-result (jdbc/execute! (get-ds) db-query)
        tf (map context_key_transform_ml db-result)]

    ;(logging/info "handle_usr-list-context_keys" "\ndb-query\n" db-query)
    (sd/response_ok tf)))

(defn handle_adm-get-context_key
  [req]
  (let [result (-> req :context_key context_key_transform_ml)]
    ;(logging/info "handle_get-context_key: result: " result)
    (sd/response_ok result)))

(defn handle_usr-get-context_key
  [req]
  (let [context_key (-> req :context_key context_key_transform_ml)
        result (dissoc context_key :admin_comment :updated_at :created_at)]
    ;(logging/info "handle_usr-get-context_key" "\nbefore\n" context_key "\nresult\n" result)
    (sd/response_ok result)))

(defn handle_create-context_keys
  [req]
  (try
    (catcher/with-logging {}

      ;(let [data (-> req :parameters :body)
      ;      ins-res (jdbc/insert! (get-ds) :context_keys data)]


        (let [data (-> req :parameters :body)
              sql-map {:insert-into :context_keys
                       :values [data]}
              sql (-> sql-map sql-format )
              ins-res (jdbc/execute! (get-ds) [sql data])]

        (sd/logwrite req (str "handle_create-context_keys: " "\new-data:\n" data "\nresult:\n" ins-res))

        (if-let [result (first ins-res)]
          (sd/response_ok (context_key_transform_ml result))
          (sd/response_failed "Could not create context_key." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-context_keys
  [req]
  (try
    (catcher/with-logging {}
      ;(let [data (-> req :parameters :body)
      ;      id (-> req :parameters :path :id)
      ;      dwid (assoc data :id id)
      ;      upd-query (sd/sql-update-clause "id" (str id))
      ;      upd-result (jdbc/update! (get-ds) :context_keys dwid upd-query)]

        (let [data (-> req :parameters :body)
              id (-> req :parameters :path :id)
              dwid (assoc data :id id)
              sql-map {:update :context_keys
                       :set dwid
                       :where [:= :id id]}
              sql (-> sql-map sql-format )
              upd-result (jdbc/execute! (get-ds) [sql (vals dwid)])]

        (sd/logwrite req (str "handle_update-context_keys: " id "\nnew-data\n" dwid "\nupd-result: " upd-result))

        (if (= 1 (first upd-result))
          (sd/response_ok (context_key_transform_ml (sd/query-eq-find-one :context_keys :id id)))
          (sd/response_failed "Could not update context_key." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_delete-context_key
  [req]
  (try
    (catcher/with-logging {}

      ;(let [context_key (-> req :context_key)
      ;      id (-> req :context_key :id)
      ;      del-query (sd/sql-update-clause "id" id)
      ;      del-result (jdbc/delete! (get-ds) :context_keys del-query)]

        (let [context_key (-> req :context_key)
              id (-> req :context_key :id)
              sql-map {:delete :context_keys
                       :where [:= :id id]}
              sql (-> sql-map sql-format )
              del-result (jdbc/execute! (get-ds) [sql [id]])]

        (sd/logwrite req (str "handle_delete-context_key: " id " result: " del-result))
        (if (= 1 (first del-result))
          (sd/response_ok (context_key_transform_ml context_key))
          (logging/error "Could not delete context_key: " id))))
    (catch Exception ex (sd/response_exception ex))))

(defn wwrap-find-context_key [param colname send404]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler
                                    param
                                    :context_keys colname
                                    :context_key send404))))

(def schema_import_context_keys
  {;:id s/Str
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   (s/optional-key :length_max) (s/maybe s/Int)
   (s/optional-key :length_min) (s/maybe s/Int)
   :position s/Int
   (s/optional-key :admin_comment) (s/maybe s/Str)
   ; hstore
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)})

(def schema_update_context_keys
  {;(s/optional-key :id) s/Str
   ;:context_id s/Str
   ;(s/optional-key :meta_key_id) s/Str
   (s/optional-key :is_required) s/Bool
   (s/optional-key :length_max) (s/maybe s/Int)
   (s/optional-key :length_min) (s/maybe s/Int)
   (s/optional-key :position) s/Int
   (s/optional-key :admin_comment) (s/maybe s/Str)
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)})

(def schema_export_context_key
  {:id s/Uuid
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   :length_max (s/maybe s/Int)
   :length_min (s/maybe s/Int)
   :position s/Int

   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)
   :hints (s/maybe sd/schema_ml_list)

   :documentation_urls (s/maybe sd/schema_ml_list)})

(def schema_export_context_key_admin
  {:id s/Uuid
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   :length_max (s/maybe s/Int)
   :length_min (s/maybe s/Int)
   :position s/Int

   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)
   :hints (s/maybe sd/schema_ml_list)

   :documentation_urls (s/maybe sd/schema_ml_list)

   :admin_comment (s/maybe s/Str)
   :updated_at s/Any
   :created_at s/Any})

; TODO docu
; TODO tests
(def admin-routes
  ["/context-keys"
   ["/"
    {:post
     {:summary (sd/sum_adm "Create context_key")
      :handler handle_create-context_keys
      :middleware [wrap-authorize-admin!]
      :content-type "application/json"
      :accept "application/json"
      :coercion reitit.coercion.schema/coercion
      :parameters {:body schema_import_context_keys}
      :responses {200 {:body schema_export_context_key_admin}
                  406 {:body s/Any}}}

    ; context_key list / query
     :get
     {:summary (sd/sum_adm "Query context_keys.")
      :handler handle_adm-list-context_keys
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :changed_after) s/Inst
                           (s/optional-key :created_after) s/Inst
                           (s/optional-key :updated_after) s/Inst
                           (s/optional-key :page) s/Int
                           (s/optional-key :count) s/Int
                           (s/optional-key :id) s/Uuid
                           (s/optional-key :context_id) s/Str
                           (s/optional-key :meta_key_id) s/Str
                           (s/optional-key :is_required) s/Bool}}
      :responses {200 {:body [schema_export_context_key_admin]}
                  406 {:body s/Any}}}}]
    ; edit context_key
   ["/:id"
    {:get
     {:summary (sd/sum_adm "Get context_key by id.")
      :handler handle_adm-get-context_key
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Str}}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :put
     {:summary (sd/sum_adm "Update context_keys with id.")
      :handler handle_update-context_keys
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Str}
                   :body schema_update_context_keys}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm_todo "Delete context_key by id.")
      :coercion reitit.coercion.schema/coercion
      :handler handle_delete-context_key
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :parameters {:path {:id s/Str}}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]])

; TODO docu
(def user-routes
  ["/context-keys"
   ["/"
    {:get
     {:summary (sd/sum_pub "Query / List context_keys.")
      :handler handle_usr-list-context_keys
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :id) s/Str
                           (s/optional-key :context_id) s/Str
                           (s/optional-key :meta_key_id) s/Str
                           (s/optional-key :is_required) s/Bool}}
      :responses {200 {:body [schema_export_context_key]}
                  406 {:body s/Any}}}}]

   ["/:id"
    {:get
     {:summary (sd/sum_pub "Get context_key by id.")
      :handler handle_usr-get-context_key
      :middleware [(wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Str}}
      :responses {200 {:body schema_export_context_key}
                  404 {:body s/Any}}}}]])