(ns madek.api.resources.meta-keys
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.meta-keys.index :as mkindex]
   [madek.api.resources.meta-keys.meta-key :as mk]
   [madek.api.resources.shared :as sd]

   ;; all needed imports
   [madek.api.resources.shared :as sd]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [madek.api.utils.rdbms :as rdbms]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.schema]
   [reitit.coercion.spec]

   [madek.api.utils.helper :refer [t d v fv f]]

   [schema.core :as s]))

(defn adm-export-meta-key [meta-key]
  (-> meta-key

      (assoc :hints (sd/transform_ml (:hints meta-key))
             :labels (sd/transform_ml (:labels meta-key))
             :descriptions (sd/transform_ml (:descriptions meta-key))
             :documentation_urls (sd/transform_ml (:documentation_urls meta-key))

             ;:labels_2 (sd/transform_ml (:labels_2 meta-key))
             ;:descriptions_2 (sd/transform_ml (:descriptions_2 meta-key))
             )))

(defn adm-export-meta-key-list [meta-key]
  (-> meta-key

      (assoc :hints (sd/transform_ml (:hints meta-key))
             :labels (sd/transform_ml (:labels meta-key))
             :descriptions (sd/transform_ml (:descriptions meta-key))
             :documentation_urls (sd/transform_ml (:documentation_urls meta-key))

             :labels_2 (sd/transform_ml (:labels_2 meta-key))
             :descriptions_2 (sd/transform_ml (:descriptions_2 meta-key)))))

(defn user-export-meta-key [meta-key]
  (-> meta-key
      (dissoc :admin_comment :admin_comment_2)
      (assoc :hints (sd/transform_ml (:hints meta-key))
             :labels (sd/transform_ml (:labels meta-key))
             :descriptions (sd/transform_ml (:descriptions meta-key))
             :documentation_urls (sd/transform_ml (:documentation_urls meta-key))

             ;:labels_2 (sd/transform_ml (:labels_2 meta-key))
             ;:descriptions_2 (sd/transform_ml (:descriptions_2 meta-key))
             )))

(defn user-export-meta-key-list [meta-key]
  (-> meta-key
      (dissoc :admin_comment :admin_comment_2)
      (assoc :hints (sd/transform_ml (:hints meta-key))
             :labels (sd/transform_ml (:labels meta-key))
             :descriptions (sd/transform_ml (:descriptions meta-key))
             :documentation_urls (sd/transform_ml (:documentation_urls meta-key))

             :labels_2 (sd/transform_ml (:labels_2 meta-key))
             :descriptions_2 (sd/transform_ml (:descriptions_2 meta-key)))))

(defn handle_adm-query-meta-keys [req]
  (let [db-result (mkindex/db-query-meta-keys req)
        result (map adm-export-meta-key-list db-result)]
    (sd/response_ok {:meta-keys result})))                  ;; TODO: add headers.x-total-count

(defn handle_usr-query-meta-keys [req]
  (let [db-result (mkindex/db-query-meta-keys req)
        result (map user-export-meta-key-list db-result)]
    (sd/response_ok {:meta-keys result})))

(defn handle_adm-get-meta-key [req]
  (let [mk (-> req :meta_key)
        result (mk/include-io-mappings
                 (adm-export-meta-key mk) (:id mk))]
    (sd/response_ok result)))

(defn handle_usr-get-meta-key [req]
  (let [mk (-> req :meta_key)
        p (println ">o> handle_usr-get-meta-key!!!! mk=" mk)
        p (println ">o> handle_usr-get-meta-key!!!! mk.class=" (class mk))
        result (mk/include-io-mappings
                 (user-export-meta-key mk) (:id mk))]
    (sd/response_ok result)))

(defn handle_create_meta-key [req]
  (let [data (-> req :parameters :body)

        sql-query (-> (sql/insert-into :meta_keys)
                      (sql/values [data])
                      (sql/returning :*)
                      sql-format)
        db-result (jdbc/execute-one! (get-ds) sql-query)]

    ;db-result (jdbc/insert! (get-ds) :meta_keys data)]

    (sd/response_ok db-result)))

(defn handle_update_meta-key [req]
  (let [;old-data (-> req :meta_key)
        data (-> req :parameters :body)
        id (-> req :parameters :path :id)
        dwid (assoc data :id id)

        sql-query (-> (sql/update :meta_keys)
                      (sql/set dwid)
                      (sql/where [:= :id id])
                      sql-format)
        db-result (jdbc/execute! (get-ds) sql-query)]

    (logging/info "handle_update_meta-key:"
      "\nid: " id
      "\ndwid\n" dwid)
    ;(if-let [db-result (jdbc/update! (get-ds)
    ;                                 :meta_keys dwid ["id = ?" id])]

    (if db-result

      (let [new-data (sd/query-eq-find-one :meta_keys :id id)]
        (logging/info "handle_update_meta-key:"
          "\ndb-result:\n" db-result
          "\nnew-data:\n" new-data)
        (sd/response_ok new-data))
      (sd/response_failed "Could not update meta_key." 406))))

(defn handle_delete_meta-key [req]
  (let [meta-key (-> req :meta_key)

        ;db-result (jdbc/delete! (get-ds) :meta_keys ["id = ?" (:id meta-key)])]

        sql-query (-> (sql/delete-from :meta_keys)
                      (sql/where [:= :id (:id meta-key)])
                      sql-format)
        db-result (jdbc/execute-one! (get-ds) sql-query)]

    (if (= 1 (::jdbc/update-count db-result))
      (sd/response_ok meta-key)
      (sd/response_failed "Could not delete meta-key." 406))))

(def schema_create-meta-key
  {:id s/Str
   :is_extensible_list s/Bool
   :meta_datum_object_type (s/enum "MetaDatum::Text"
                             "MetaDatum::TextDate"
                             "MetaDatum::JSON"
                             "MetaDatum::Keywords"
                             "MetaDatum::People"
                             "MetaDatum::Roles")
   (s/optional-key :keywords_alphabetical_order) s/Bool
   (s/optional-key :position) s/Int
   :is_enabled_for_media_entries s/Bool
   :is_enabled_for_collections s/Bool
   :vocabulary_id s/Str

   (s/optional-key :allowed_people_subtypes) [(s/enum "People" "PeopleGroup")] ; TODO check more people subtypes?!?
   (s/optional-key :text_type) s/Str
   (s/optional-key :allowed_rdf_class) (s/maybe s/Str)

   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)

   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_update-meta-key
  {;:id s/Str
   (s/optional-key :is_extensible_list) s/Bool
   ;:meta_datum_object_type (s/enum "MetaDatum::Text"
   ;                                "MetaDatum::TextDate"
   ;                                "MetaDatum::JSON"
   ;                                "MetaDatum::Keywords"
   ;                                "MetaDatum::People"
   ;                                "MetaDatum::Roles")
   (s/optional-key :keywords_alphabetical_order) s/Bool
   (s/optional-key :position) s/Int
   (s/optional-key :is_enabled_for_media_entries) s/Bool
   (s/optional-key :is_enabled_for_collections) s/Bool
   ;:vocabulary_id s/Str

   (s/optional-key :allowed_people_subtypes) [(s/enum "People" "PeopleGroup")] ; TODO check more people subtypes?!?
   (s/optional-key :text_type) s/Str                        ; TODO enum
   (s/optional-key :allowed_rdf_class) (s/maybe s/Str)

   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)

   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_export-meta-key-usr
  {:id s/Str
   (s/optional-key :is_extensible_list) s/Bool
   (s/optional-key :meta_datum_object_type) s/Str
   (s/optional-key :keywords_alphabetical_order) s/Bool
   (s/optional-key :position) s/Int
   (s/optional-key :is_enabled_for_media_entries) s/Bool
   (s/optional-key :is_enabled_for_collections) s/Bool
   :vocabulary_id s/Str

   (s/optional-key :allowed_people_subtypes) [s/Str]
   (s/optional-key :text_type) s/Str
   (s/optional-key :allowed_rdf_class) (s/maybe s/Str)

   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)
   :hints (s/maybe sd/schema_ml_list)
   :documentation_urls (s/maybe sd/schema_ml_list)

   ;:admin_comment (s/maybe s/Str)
   (s/optional-key :io_mappings) s/Any

   (s/optional-key :enabled_for_public_use) s/Bool
   (s/optional-key :enabled_for_public_view) s/Bool
   (s/optional-key :position_2) s/Int
   (s/optional-key :labels_2) s/Any
   (s/optional-key :descriptions_2) s/Any
   (s/optional-key :id_2) s/Str
   ;:admin_comment_2 (s/maybe s/Str)
   })

(def schema_export-meta-key-adm
  (assoc schema_export-meta-key-usr
         :admin_comment (s/maybe s/Str)
         (s/optional-key :admin_comment_2) (s/maybe s/Str)))

(def schema_query-meta-key
  {(s/optional-key :id) s/Str
   (s/optional-key :vocabulary_id) s/Str
   (s/optional-key :meta_datum_object_type) s/Str           ; TODO enum
   (s/optional-key :is_enabled_for_collections) s/Bool
   (s/optional-key :is_enabled_for_media_entries) s/Bool
   (s/optional-key :scope) (s/enum "view" "use")

   ;(s/required-key :page) s/Int
   ;(s/required-key :count) s/Int

   ;:page s/Int
   ;:count s/Int
   })

(defn wwrap-find-meta_key [param colname send404]
  (fn [handler]
    (fn [request] (sd/req-find-data-new request handler
                    param
                    :meta_keys colname
                    :meta_key send404))))


(defn generate-swagger-pagination-params []
  {:produces "application/json"
   :parameters [{:name "page"
                 :in "query"
                 :description "Page number, defaults to 1"
                 :required true
                 :value 1
                 :default 1
                 :type "number"
                 :pattern "^[1-9][0-9]*$"
                 }
                {:name "count"
                 :in "query"
                 :description "Number of items per page, defaults to 100"
                 :required true
                 :value 100
                 :default 100
                 :type "number"
                 :pattern "^[1-9][0-9]*$"
                 }
                ]})

(def admin-routes
  ["/meta-keys"
   {:swagger {:tags ["admin/meta-keys"]}}
   ["/"
    {:get {:summary (sd/sum_adm (f (t "Get all meta-key ids")))
           :description "Get list of meta-key ids. Paging is used as you get a limit of 100 entries."
           :handler handle_adm-query-meta-keys
           :middleware [wrap-authorize-admin!]
           ;:swagger {:produces "application/json"}

           :swagger (generate-swagger-pagination-params)

           ; FIXME: returns vocabulary.id instead of meta-keys.id ??


           :parameters {:query schema_query-meta-key}
           :content-type "application/json"
           :coercion reitit.coercion.schema/coercion
           :responses {
                       200 {:description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
                            :body {:meta-keys [schema_export-meta-key-adm]}}

                       }}

     :post {:summary (sd/sum_adm "Create meta-key.")
            :handler handle_create_meta-key
            :middleware [wrap-authorize-admin!]
            :swagger {:produces "application/json" :consumes "application/json"}
            :parameters {:body schema_create-meta-key}
            :content-type "application/json"
            :coercion reitit.coercion.schema/coercion
            :responses {200 {:body s/Any}                   ; TODO response coersion
                        406 {:body s/Any}}}}]

   ["/:id"
    {:get {:summary (sd/sum_adm "Get meta-key by id")
           :description "Get meta-key by id. Returns 404, if no such meta-key exists."
           :swagger {:produces "application/json"}
           :content-type "application/json"
           :accept "application/json"
           :middleware [wrap-authorize-admin!
                        (sd/wrap-check-valid-meta-key :id)
                        (wwrap-find-meta_key :id :id true)]
           :handler handle_adm-get-meta-key
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Str}}
           :responses {200 {:body schema_export-meta-key-adm}
                       404 {:body {:message s/Str}}
                       422 {:body {:message s/Str}}}}

     :put {:summary (sd/sum_adm "Update meta-key.")
           :handler handle_update_meta-key
           :swagger {:produces "application/json" :consumes "application/json"}
           :middleware [wrap-authorize-admin!
                        (sd/wrap-check-valid-meta-key :id)
                        (wwrap-find-meta_key :id :id true)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Str}
                        :body schema_update-meta-key}
           :responses {200 {:body s/Any}                    ; TODO response coercion
                       404 {:body {:message s/Str}}
                       422 {:body {:message s/Str}}}}

     :delete {:summary (sd/sum_adm "Delete meta-key.")
              :handler handle_delete_meta-key
              :swagger {:produces "application/json" :consumes "application/json"}
              :middleware [(sd/wrap-check-valid-meta-key :id)
                           (wwrap-find-meta_key :id :id true)]
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:id s/Str}}
              :responses {200 {:body schema_export-meta-key-adm}
                          404 {:body {:message s/Str}}
                          422 {:body {:message s/Str}}}}}]])

; TODO tests
(def query-routes
  ["/meta-keys"
   {:swagger {:tags ["meta-keys"]}}
   ["/"
    {:get {:summary (sd/sum_usr_pub (f (t "Get all meta-key ids")))
           :description "Get list of meta-key ids. Paging is used as you get a limit of 100 entries."
           :handler handle_usr-query-meta-keys
           :parameters {:query schema_query-meta-key}
           :swagger (generate-swagger-pagination-params)


           ; FIXME: returns vocabulary.id instead of meta-keys.id ??

           :content-type "application/json"
           :coercion reitit.coercion.schema/coercion

           ; TODO or better own link
           :responses {
                       200 {
                            :description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
                            :body {:meta-keys [schema_export-meta-key-usr]}
                            }
                       }}}]

   ["/:id"
    {:get {:summary (sd/sum_usr_pub (v (t "Get meta-key by id")))
           :description "Get meta-key by id. Returns 404, if no such meta-key exists."
           :content-type "application/json"
           :accept "application/json"
           :handler handle_usr-get-meta-key
           ;
           :middleware [(sd/wrap-check-valid-meta-key-new :id)
                        (wwrap-find-meta_key :id :id true)]
           :coercion reitit.coercion.schema/coercion

           :swagger {:produces "application/json"
                     :parameters [{:name "id"
                                   :in "path"
                                   :description "e.g.: madek_core:subtitle"
                                   :type "string"
                                   :required true
                                   :pattern "^[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+$"
                                   }]}

           :responses {200 {:body schema_export-meta-key-usr}

                       404 {:description "No entry found for the given id"
                            :schema s/Str
                            :examples {"application/json" {:message "No such entity in :meta_keys as :id with not-existing:key"}}}

                       422 {:body {:message s/Str}}}}}]])
;### Debug ####################################################################
;(debug/debug-ns *ns*)
