(ns madek.api.resources.collections
  (:require [clojure.java.jdbc :as jdbco]
            [clojure.tools.logging :as logging]
            [logbug.catcher :as catcher]
            [madek.api.authorization :as authorization]
            [madek.api.resources.collections.index :refer [get-index]]
            [madek.api.resources.shared :as sd]
            [madek.api.utils.rdbms :as rdbms]
            [reitit.coercion.schema]

                  ;; all needed imports
                        [honey.sql :refer [format] :rename {format sql-format}]
                        ;[leihs.core.db :as db]
                        [next.jdbc :as jdbc]
                        [honey.sql.helpers :as sql]

                        [madek.api.db.core :refer [get-ds]]

            [schema.core :as s]))

(defn handle_get-collection [request]
  (let [collection (:media-resource request)
        cleanedcol (dissoc collection :table-name :type
                           ;:responsible_delegation_id
                           ; TODO Frage cipboard_user
                           ;:clipboard_user_id
                           )]
    (sd/response_ok cleanedcol)))

(defn handle_get-index [req]
  (let [query-params (-> req :parameters :query)
        qreq (assoc-in req [:query-params] query-params)]
    (logging/info "handle_get-index" "\nquery-params\n" query-params)
    (get-index qreq)))

(defn handle_create-collection [req]
  (try
    (catcher/with-logging {}
      (if-let [auth-id (-> req :authenticated-entity :id)]

        ;(let [req-data (-> req :parameters :body)
        ;      ins-data (assoc req-data :creator_id auth-id :responsible_user_id auth-id)
        ;      ins-result (jdbc/insert! (rdbms/get-ds) "collections" ins-data)]

          (let [req-data (-> req :parameters :body)
                ins-data (assoc req-data :creator_id auth-id :responsible_user_id auth-id)
                sql-map {:insert-into :collections
                         :values [ins-data]}
                sql (-> sql-map sql-format )
                ins-result (jdbc/execute! (get-ds) [sql ins-data])]

          (sd/logwrite req (str "handle_create-collection: " ins-result))
          (if-let [result (first ins-result)]
            (sd/response_ok result)
            (sd/response_failed "Could not create collection" 406)))
        (sd/response_failed "Could not create collection. Not logged in." 406)))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-collection [req]
  (try
    (catcher/with-logging {}

      ;(let [collection (:media-resource req)
      ;      col-id (:id collection)
      ;      data (-> req :parameters :body)
      ;      whcl ["id = ? " col-id]
      ;      result (jdbc/update! (rdbms/get-ds) :collections data whcl)]

        (let [collection (:media-resource req)
              col-id (:id collection)
              data (-> req :parameters :body)
              sql-map {:update :collections
                       :set data
                       :where [:= :id col-id]}
              sql (-> sql-map sql-format )
              params (concat (vals data) [col-id])
              result (jdbc/execute! (get-ds) [sql params])]

        (sd/logwrite req (str "handle_update-collection: " col-id result))

        (if (= 1 (first result))
          (sd/response_ok (sd/query-eq-find-one :collections :id col-id))
          (sd/response_failed "Could not update collection." 422))))
    (catch Exception ex
      (sd/response_exception ex))))

(defn handle_delete-collection [req]
  (try
    (catcher/with-logging {}

      ;(let [collection (:media-resource req)
      ;      col-id (:id collection)
      ;      delquery ["id = ? " col-id]
      ;      delresult (jdbc/delete! (rdbms/get-ds) :collections delquery)]

        (let [collection (:media-resource req)
              col-id (:id collection)
              sql-map {:delete :collections
                       :where [:= :id col-id]}
              sql (-> sql-map sql-format )
              delresult (jdbc/execute! (get-ds) [sql [col-id]])]


        (sd/logwrite req (str "handle_delete-collection: " col-id delresult))
        (if (= 1 (first delresult))
          (sd/response_ok (dissoc collection :type :table-name))
          (sd/response_failed (str "Could not delete collection: " col-id) 422))))
    (catch Exception ex
      (sd/response_failed (str "Could not delete collection: " (ex-message ex)) 500))))

; TODO :layout and :sorting are special types
(def schema_layout_types
  (s/enum "grid" "list" "miniature" "tiles"))

(def schema_sorting_types
  (s/enum "created_at ASC"
          "created_at DESC"
          "title ASC"
          "title DESC"
          "last_change"
          "manual ASC"
          "manual DESC"))

(def schema_default_resource_type
  (s/enum "collection" "entries" "all"))

(def schema_collection-import
  {;(s/optional-key :id) s/Uuid
   (s/optional-key :get_metadata_and_previews) s/Bool

   (s/optional-key :layout) schema_layout_types
   (s/optional-key :is_master) s/Bool
   (s/optional-key :sorting) schema_sorting_types
   (s/optional-key :default_context_id) (s/maybe s/Uuid)
   (s/optional-key :responsible_user_id) s/Uuid
   ;(s/optional-key :clipboard_user_id) (s/maybe s/Uuid)
   (s/optional-key :workflow_id) (s/maybe s/Uuid)
   (s/optional-key :responsible_delegation_id) (s/maybe s/Uuid)

   (s/optional-key :default_resource_type) schema_default_resource_type})

(def schema_collection-update
  {(s/optional-key :layout) schema_layout_types
   (s/optional-key :is_master) s/Bool
   (s/optional-key :sorting) schema_sorting_types
   (s/optional-key :default_context_id) (s/maybe s/Uuid)

   ;(s/optional-key :get_metadata_and_previews) s/Bool
   ;(s/optional-key :responsible_user_id) s/Uuid

   ;(s/optional-key :clipboard_user_id) (s/maybe s/Uuid)
   (s/optional-key :workflow_id) (s/maybe s/Uuid)
   ;(s/optional-key :responsible_delegation_id) (s/maybe s/Uuid)

   (s/optional-key :default_resource_type) schema_default_resource_type})

(def schema_collection-query
  {(s/optional-key :page) s/Int
   (s/optional-key :count) s/Int
   (s/optional-key :full_data) s/Bool
   (s/optional-key :collection_id) s/Str
   (s/optional-key :order) s/Str

   (s/optional-key :creator_id) s/Uuid
   (s/optional-key :responsible_user_id) s/Uuid

   (s/optional-key :clipboard_user_id) s/Uuid
   (s/optional-key :workflow_id) s/Uuid
   (s/optional-key :responsible_delegation_id) s/Uuid

   (s/optional-key :public_get_metadata_and_previews) s/Bool
   (s/optional-key :me_get_metadata_and_previews) s/Bool
   (s/optional-key :me_edit_permission) s/Bool
   (s/optional-key :me_edit_metadata_and_relations) s/Bool})

(def schema_collection-export
  {:id s/Uuid
   (s/optional-key :get_metadata_and_previews) s/Bool

   (s/optional-key :layout) schema_layout_types
   (s/optional-key :is_master) s/Bool
   (s/optional-key :sorting) schema_sorting_types

   (s/optional-key :responsible_user_id) (s/maybe s/Uuid)
   (s/optional-key :creator_id) s/Uuid

   (s/optional-key :default_context_id) (s/maybe s/Uuid)

   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any
   (s/optional-key :meta_data_updated_at) s/Any
   (s/optional-key :edit_session_updated_at) s/Any

   (s/optional-key :clipboard_user_id) (s/maybe s/Uuid)
   (s/optional-key :workflow_id) (s/maybe s/Uuid)
   (s/optional-key :responsible_delegation_id) (s/maybe s/Uuid)

   (s/optional-key :default_resource_type) schema_default_resource_type})

(def ring-routes
  ["/"
   ["collections"
    {:get
     {:summary (sd/sum_usr "Query/List collections.")
      :handler handle_get-index
      :swagger {:produces "application/json"}
      :parameters {:query schema_collection-query}
      :coercion reitit.coercion.schema/coercion
      :responses {200 {:body {:collections [schema_collection-export]}}}}}]

   ["collection"
    {:post
     {:summary (sd/sum_usr "Create collection")
      :handler handle_create-collection
      :swagger {:produces "application/json"
                :consumes "application/json"}
      :parameters {:body schema_collection-import}
      :middleware [authorization/wrap-authorized-user]
      :coercion reitit.coercion.schema/coercion
      :responses {200 {:body schema_collection-export}
                  406 {:body s/Any}}}}]

   ["collection/:collection_id"
    {:get {:summary (sd/sum_usr_pub "Get collection for id.")
           :handler handle_get-collection
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-view]

           :swagger {:produces "application/json"}
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Uuid}}
           :responses {200 {:body schema_collection-export}
                       404 {:body s/Any}
                       422 {:body s/Any}}}

     :put {:summary (sd/sum_usr "Update collection for id.")
           :handler handle_update-collection
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-edit-metadata]
           :swagger {:produces "application/json"
                     :consumes "application/json"}
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Uuid}
                        :body schema_collection-update}
           :responses {200 {:body schema_collection-export}
                       404 {:body s/Any}
                       422 {:body s/Any}}}

     ; TODO Frage: wer darf eine col löschen: nur der benutzer und der responsible
     ; TODO check owner or responsible
     :delete {:summary (sd/sum_usr "Delete collection for id.")
              :handler handle_delete-collection
              :middleware [sd/ring-wrap-add-media-resource
                           sd/ring-wrap-authorization-edit-permissions]
              :swagger {:produces "application/json"
                        :consumes "application/json"}
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:collection_id s/Uuid}}
              :responses {200 {:body schema_collection-export}
                          404 {:body s/Any}
                          422 {:body s/Any}}}}]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
