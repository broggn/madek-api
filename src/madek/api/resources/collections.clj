(ns madek.api.resources.collections
  (:require [clojure.java.jdbc :as jdbco]
            [clojure.tools.logging :as logging]
   ;; all needed imports
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [logbug.catcher :as catcher]
            [madek.api.authorization :as authorization]

            [madek.api.db.core :refer [get-ds]]

            [madek.api.resources.collections.index :refer [get-index]]
            [madek.api.resources.shared :as sd]

            [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]

            [madek.api.utils.helper :refer [mslurp]]
            [madek.api.utils.rdbms :as rdbms]
   ;[leihs.core.db :as db]
            [next.jdbc :as jdbc]

            [reitit.coercion.schema]

            [schema.core :as s]

            [taoensso.timbre :refer [debug info warn error spy]]))

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

        p (println ">o> query-params" query-params)

        qreq (assoc-in req [:query-params] query-params)
        p (println ">o> qreq" qreq)]

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

              p (println ">o> original-data=" req-data)

;{:responsible_user_id nil, :is_master true, :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3",
              ; :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6", :layout {:responsible_user_id nil, :is_master true,
              ;                                                                     :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3", :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6", :layout [:cast list :public.collection_layout], :default_context_id "columns", :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :get_metadata_and_previews true, :default_resource_type collections, :sorting manual DESC}, :default_context_id columns, :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :get_metadata_and_previews true, :default_resource_type {:responsible_user_id nil, :is_master true, :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3", :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6", :layout list, :default_context_id columns, :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :get_metadata_and_previews true, :default_resource_type [:cast collections :public.collection_default_resource_type], :sorting manual DESC}, :sorting {:responsible_user_id nil, :is_master true, :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3", :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6", :layout list, :default_context_id columns, :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :get_metadata_and_previews true, :default_resource_type collections, :sorting [:cast manual DESC :public.collection_sorting]}}

;; TODO: CAUTION
              ;; FIX OF BROKEN LOGIC - DB-CONSTRAINT ALLOWS ONLY ONE UUID FOR responsible_user_id OR responsible_delegation_id
              ;ins-data (assoc req-data :creator_id auth-id :responsible_user_id auth-id)
              ins-data (assoc req-data :creator_id auth-id)

              ins-data (convert-map-if-exist ins-data)

              p (println ">o> data=" ins-data)

              sql-map {:insert-into :collections
                       :values [ins-data]
                       :returning :*}

              sql (-> sql-map sql-format)
              ;ins-result (jdbc/execute! (get-ds) [sql ins-data])]
              ins-result (jdbc/execute! (get-ds) sql)]

          (sd/logwrite req (str "handle_create-collection: " ins-result))
          (if-let [result (first ins-result)]
            (sd/response_ok result)
            (sd/response_failed "Could not create collection" 406)))
        (sd/response_failed "Could not create collection. Not logged in." 406)))
    (catch Exception ex (sd/parsed_response_exception ex))))

(comment

  (let [{:responsible_user_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :is_master true,
         :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3", :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6",
         :layout [:cast "list" :public.collection_layout], :default_context_id "columns", :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0",
         :get_metadata_and_previews true, :default_resource_type "collections", :sorting "manual DESC"}

        {:responsible_user_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :is_master true, :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3",
         :workflow_id #uuid "3fa85f64-5717-4562-b3fc-2c963f66afa6", :layout [:cast "list" :public.collection_layout], :default_context_id "columns",
         :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0", :get_metadata_and_previews true, :default_resource_type "collections", :sorting "manual DESC"}

        params {;; CAUTION: Either :responsible_user_id OR :responsible_user_id has to be set - not both (db-constraint)

                ;:responsible_user_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0",
                ;:responsible_delegation_id nil,

                ;OR

                :responsible_user_id nil,
                :responsible_delegation_id #uuid "9f52df0d-6688-4512-81f7-d4f4eb0ec6e3"

                :creator_id #uuid "47da46e9-8a5f-4eac-a7c0-056706a70fc0",
                :default_context_id "columns",
                :workflow_id #uuid "1343d71c-4db6-4808-9a56-6933e3a1818f",

                :is_master true

                :layout [:cast "list" :public.collection_layout]

                :get_metadata_and_previews true

                :default_resource_type [:cast "collections" :public.collection_default_resource_type],

                :sorting [:cast "manual DESC" :public.collection_sorting]}

;; next.jdbc
        ;    [madek.api.db.core :refer [get-ds]]
        ;    [honey.sql :refer [format] :rename {format sql-format}]
        res (->> (jdbc/execute-one! (get-ds) (-> (sql/insert-into :collections)
                                                 (sql/values [params])
                                                 (sql/returning :*)
                                                 sql-format
                                                 spy)))]

    res))

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

            params (concat (vals data) [col-id])
            p (println ">o> wtf-is-this??? params=" params)

            ;sql-map {:update :collections
            ;         :set data
            ;         :where [:= :id col-id]}

            query (-> (sql/update :collections)
                      (sql/set (convert-map-if-exist data))
                      (sql/where [:= :id col-id])
                      (sql/returning :*)
                      sql-format)

;result (jdbc/execute! (get-ds) [sql params])]   ;;broken
            result (jdbc/execute! (get-ds) query)] ;;broken

        (sd/logwrite req (str "handle_update-collection: " col-id result))

        ;(if (= 1 (first result))
        (if result
          ;(sd/response_ok (sd/query-eq-find-one :collections :id col-id))
          (sd/response_ok result)
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
            ;sql-map {:delete :collections
            ;         :where [:= :id col-id]}
            ;sql (-> sql-map sql-format)

            query (-> (sql/delete-from :collections)
                      (sql/where [:= :id col-id])
                      (sql/returning :*)
                      sql-format)

            p (println ">o> whatsThis?? 1=" (get-in collection [:type :table-name]))
            p (println ">o> whatsThis?? 2=" (:type collection))

            ;delresult (jdbc/execute! (get-ds) [sql [col-id]])
            delresult (jdbc/execute-one! (get-ds) query)]

        (sd/logwrite req (str "handle_delete-collection: " col-id delresult))
        ;(if (= 1 (first delresult))
        (if delresult
          ;(sd/response_ok (dissoc collection :type :table-name))
          (sd/response_ok delresult)
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
  (s/enum "collections" "entries" "all"))

(def schema_collection-import
  {;(s/optional-key :id) s/Uuid
   (s/optional-key :get_metadata_and_previews) s/Bool

   (s/optional-key :layout) schema_layout_types
   (s/optional-key :is_master) s/Bool
   (s/optional-key :sorting) schema_sorting_types
   (s/optional-key :default_context_id) (s/maybe s/Str) ;;cautioin
   ;(s/optional-key :clipboard_user_id) (s/maybe s/Uuid)
   (s/optional-key :workflow_id) (s/maybe s/Uuid)

   ;; only one should be set (uuid & null check)
   (s/optional-key :responsible_user_id) (s/maybe s/Uuid)
   (s/optional-key :responsible_delegation_id) (s/maybe s/Uuid)

   (s/optional-key :default_resource_type) schema_default_resource_type})

(def schema_collection-update
  {(s/optional-key :layout) schema_layout_types
   (s/optional-key :is_master) s/Bool
   (s/optional-key :sorting) schema_sorting_types
   (s/optional-key :default_context_id) (s/maybe s/Str)

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
   (s/optional-key :collection_id) s/Uuid
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

   (s/optional-key :default_context_id) (s/maybe s/Str)

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
   {:swagger {:tags ["collection"]}}
   ["collections"
    {:get
     {:summary (sd/sum_usr (f (t "Query/List collections.") "BROKEN-FILTER"))
      :handler handle_get-index
      :swagger {:produces ["application/json" "application/octet-stream"]}
      :parameters {:query schema_collection-query}
      :coercion reitit.coercion.schema/coercion
      :responses {200 {:body {:collections [schema_collection-export]}}}}}]

   ["collection"
    {:post
     {:summary (sd/sum_usr (t "Create collection"))

      ;:description "CAUTION: Either :responsible_user_id OR :responsible_user_id has to be set - not both (db-constraint)"

      :description (mslurp "./md/collections-post.md")

      :handler handle_create-collection
      :swagger {:produces "application/json"
                :consumes "application/json"}
      :parameters {:body schema_collection-import}
      :middleware [authorization/wrap-authorized-user]
      :coercion reitit.coercion.schema/coercion
      :responses {200 {:body schema_collection-export}
                  406 {:body s/Any}}}}]

   ["collection/:collection_id"
    {:get {:summary (sd/sum_usr_pub (t "Get collection for id."))
           :handler handle_get-collection
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-view]

           :swagger {:produces "application/json"}
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Uuid}}
           :responses {200 {:body schema_collection-export}
                       404 {:body s/Any}
                       422 {:body s/Any}}}

     :put {:summary (sd/sum_usr (t "Update collection for id."))
           :handler handle_update-collection
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-edit-metadata]
           :swagger {:produces "application/json"
                     :consumes "application/json"}
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Uuid}
                        :body schema_collection-update}
           :responses {;200 {:body schema_collection-export} ;; TODO: fixme
                       200 {:body s/Any}
                       404 {:body s/Any}
                       422 {:body s/Any}}}

     ; TODO Frage: wer darf eine col löschen: nur der benutzer und der responsible
     ; TODO check owner or responsible
     :delete {:summary (sd/sum_usr (t "Delete collection for id."))
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
