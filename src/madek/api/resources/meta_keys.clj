(ns madek.api.resources.meta-keys
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]
   ;; all needed imports
   [clojure.tools.logging :as logging]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.meta-keys.index :as mkindex]
   [madek.api.resources.meta-keys.meta-key :as mk]

   ;; all needed imports
   [madek.api.resources.shared :as sd]

   [madek.api.resources.shared :as sd]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]

   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist f replace-java-hashmaps t v]]
   [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]

   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [schema.core :as s])

  ;(:import (java.util.HashMap))
  )
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
    (sd/response_ok {:meta-keys result}))) ;; TODO: add headers.x-total-count

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

(comment
  (let [map {:hints (java.util.HashMap. {"de" "string1" "en" "string"})
             :labels (java.util.HashMap. {"de" "string2" "en" "string"})
             :is_enabled_for_collections true
             :allowed_rdf_class :Keyword
             :documentation_urls (java.util.HashMap. {"de" "string3" "en" "string"})
             :is_enabled_for_media_entries true
             :keywords_alphabetical_order true
             :id "copyright:test_me_now1003"
             :meta_datum_object_type :MetaDatum/TextDate
             :vocabulary_id :copyright
             :admin_comment "string"
             :position 0
             :is_extensible_list true
             :descriptions (java.util.HashMap. {"de" "string4" "en" "string"})
             :allowed_people_subtypes ["People"]
             :text_type :line}

        ;map {:hints  (java.util.HashMap. {"de" "string1" "en" "string"})}

        clj-map map
        p (println ">o> clj-map1" clj-map)

        clj-map (replace-java-hashmaps map)

        p (println ">o> clj-map1" clj-map)
        p (println ">o> clj-map1" (class (:hints clj-map)))]

    clj-map))

(defn handle_create_meta-key [req]
  (let [data (-> req :parameters :body)

        p (println ">o> handle_create_meta-key!!!! data=" data)

        sql-query (-> (sql/insert-into :meta_keys)
                      (sql/values [(convert-map-if-exist (cast-to-hstore data))])
                      (sql/returning :*)
                      sql-format)
        p (println ">o> handle_create_meta-key!!!! sql-query=" sql-query)

        db-result (jdbc/execute-one! (get-ds) sql-query)
        p (println ">o> db-result1" db-result)

        db-result (replace-java-hashmaps db-result)

        p (println ">o> db-result1x" db-result)
        p (println ">o> db-result1y" (class (:hints db-result)))
        p (println ">o> db-result2z" (json/generate-string db-result))]

    (sd/response_ok db-result)))

(comment

  (let [data {:meta_datum_object_type [:cast "MetaDatum::TextDate" :text]
              :is_extensible_list true
              :is_enabled_for_collections true
              :vocabulary_id "copyright"
              :is_enabled_for_media_entries true
              :id "copyright:test_me_now"}

        data {:descriptions {:de "string" :en "string"}
              :meta_datum_object_type "MetaDatum::TextDate"
              :is_extensible_list true
              :is_enabled_for_collections true
              :allowed_rdf_class "Keyword"
              :documentation_urls {:de "string" :en "string"}
              :vocabulary_id "copyright"
              :is_enabled_for_media_entries true
              :id "copyright:test_me_now10"
              :position 0
              :admin_comment "string"
              :labels {:de "string" :en "string"}
              :hints {:de "string" :en "string"}
              :keywords_alphabetical_order true
              :text_type "line" ;; OR block
              :allowed_people_subtypes ["People"]}
        ;; TODO: get rid of cast-fnc
        data (convert-map-if-exist (cast-to-hstore data))

        p (println ">o> data=" data)

        sql-query (-> (sql/insert-into :meta_keys)
                      (sql/values [data])
                      (sql/returning :*)
                      sql-format)

        p (println ">o> handle_create_meta-key!!!! sql-query=" sql-query)

        db-result (jdbc/execute-one! (get-ds) sql-query)]

    p (println "\nquery" sql-query)
    p (println "\nquery2" db-result)))

(defn handle_update_meta-key [req]
  (let [;old-data (-> req :meta_key)
        data (-> req :parameters :body)
        id (-> req :path-params :id)
        dwid (assoc data :id id)

        p (println ">o> dwid1=" dwid)

        p (println ">o> dwid1.id=" id)

        dwid (convert-map-if-exist (cast-to-hstore dwid))
        p (println ">o> dwid1.converted=" dwid)

        sql-query (-> (sql/update :meta_keys)

                      (sql/set dwid)

                      (sql/returning :*)

                      (sql/where [:= :id id])
                      sql-format)

        p (println ">o> dwid2=" dwid)
        p (println ">o> sql-query=" sql-query)

        db-result (jdbc/execute-one! (get-ds) sql-query)
        db-result (replace-java-hashmaps db-result)

        p (println ">o> db-result=" db-result)]

    (logging/info "handle_update_meta-key:"
                  "\nid: " id
                  "\ndwid\n" dwid)
    ;(if-let [db-result (jdbc/update! (get-ds)
    ;                                 :meta_keys dwid ["id = ?" id])]

    (if db-result

      ;(let [new-data (sd/query-eq-find-one :meta_keys :id id)]
      (do
        (logging/info "handle_update_meta-key:"
          ;"\ndb-result:\n" db-result
                      "\nnew-data:\n" db-result)
        (sd/response_ok db-result))
      (sd/response_failed "Could not update meta_key." 406))))

(comment

  (let [id "copyright:test_me_now"

        data {:hints {:de "string-11", :en "string-11"}, :labels {:de "string-11", :en "string-11"}, :is_enabled_for_collections true,
              :allowed_rdf_class "Keyword", :documentation_urls {:de "string-11", :en "string-11"}, :is_enabled_for_media_entries true,
              :keywords_alphabetical_order true,
              ;:id id,
              :admin_comment "string-11", :position 0, :is_extensible_list true,
              :descriptions {:de "string-11", :en "string-11"}, :allowed_people_subtypes ["People"], :text_type "line"}

        data (convert-map-if-exist (cast-to-hstore data))
        p (println ">o> dwid1.converted=" data)

        sql-query (-> (sql/update :meta_keys)

                      (sql/set data)

                      (sql/returning :*)

                      (sql/where [:= :id id])
                      sql-format)

        p (println ">o> dwid2=" data)
        p (println ">o> sql-query=" sql-query)

        res (jdbc/execute-one! (get-ds) sql-query)]

    res))

(defn handle_delete_meta-key [req]
  (let [id (-> req :path-params :id)
        p (println ">o> id=" id)

        sql-query (-> (sql/delete-from :meta_keys)
                      (sql/where [:= :id id])
                      (sql/returning :*)
                      sql-format)

        p (println ">o> handle_delete_meta-key sql-query3=" sql-query)

        db-result (jdbc/execute-one! (get-ds) sql-query)

        p (println ">o> handle_delete_meta-key db-result4=" db-result)]

;(if (= 1 (::jdbc/update-count db-result))
    (if db-result
      (sd/response_ok db-result)
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
   (s/optional-key :text_type) s/Str ; TODO enum
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
   (s/optional-key :meta_datum_object_type) s/Str ; TODO enum
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

;; TODO: move to shared
(defn generate-swagger-pagination-params []
  {:produces "application/json"
   :parameters [{:name "page"
                 :in "query"
                 :description "Page number, defaults to 1"
                 :required true
                 :value 1
                 :default 1
                 :type "number"
                 :pattern "^[1-9][0-9]*$"}
                {:name "count"
                 :in "query"
                 :description "Number of items per page, defaults to 100"
                 :required true
                 :value 100
                 :default 100
                 :type "number"
                 :pattern "^[1-9][0-9]*$"}]})

(def admin-routes
  ["/meta-keys"
   {:swagger {:tags ["admin/meta-keys"] :security [{"auth" []}]}}
   ["/"
    {:get {:summary (sd/sum_adm (f (t "Get all meta-key ids")))
           :description "Get list of meta-key ids. Paging is used as you get a limit of 100 entries."
           :handler handle_adm-query-meta-keys
           :middleware [wrap-authorize-admin!]
           :swagger (generate-swagger-pagination-params)

           ; FIXME: returns vocabulary.id instead of meta-keys.id ??

           :parameters {:query schema_query-meta-key}
           :content-type "application/json"
           :coercion reitit.coercion.schema/coercion
           :responses {200 {:description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
                            :body {:meta-keys [schema_export-meta-key-adm]}}}}

     ;["/test-me"
     ; ;{:swagger {:tags ["test"] :security [{"auth" []}]}}
     ; ;; TEST
     ; {:post {:summary (sd/sum_adm (f (t "Get all meta-key ids")))
     ;      :description "Get list of meta-key ids. Paging is used as you get a limit of 100 entries."
     ;      :handler handle_adm-query-meta-keys
     ;      :middleware [wrap-authorize-admin!]
     ;      :swagger (generate-swagger-pagination-params)
     ;
     ;      ; FIXME: returns vocabulary.id instead of meta-keys.id ??
     ;
     ;      :parameters {:query schema_query-meta-key
     ;                   :body schema_create-meta-key
     ;                   }
     ;
     ;      :content-type "application/json"
     ;      :coercion reitit.coercion.schema/coercion
     ;      :responses {200 {:description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
     ;                       :body {:meta-keys [schema_export-meta-key-adm]}}}}}

     :post {:summary (sd/sum_adm (t "Create meta-key."))
            :handler handle_create_meta-key
            :middleware [wrap-authorize-admin!]

            :description (slurp "./md/meta-key-post.md")

            :parameters {:body schema_create-meta-key}

            :content-type "application/json"
            :coercion reitit.coercion.schema/coercion
            :responses {200 {:body schema_create-meta-key}

                        404 {:description "Duplicate key error"
                             :schema s/Str
                             :examples {"application/json" {:msg "ERROR: duplicate key value violates unique constraint \\\"meta_keys_pkey\\\"\\n  Detail: Key (id)=(copyright:test_me_now31) already exists."}}}

                        500 {:description "Internal Server Error"
                             :schema s/Str
                             :examples {"application/json" {:msg "ERROR: new row for relation \"meta_keys\" violates check constraint \"meta_key_id_chars\"\n  Detail: Failing row contains (copyright-test_me_now10, t, MetaDatum::TextDate, t, 0, t, t, copyright, string, {People}, line, Keyword, \"de\"=>\"string\", \"en\"=>\"string\", \"de\"=>\"string\", \"en\"=>\"string\", \"de\"=>\"string\", \"en\"=>\"string\", \"de\"=>\"string\", \"en\"=>\"string\")."}}}

                        406 {:body s/Any}}}


       }]

   ["/:id"
    {:get {:summary (sd/sum_adm (t "Get meta-key by id"))
           :description "Get meta-key by id. Returns 404, if no such meta-key exists."
           :content-type "application/json"
           :accept "application/json"
           :middleware [wrap-authorize-admin!
                        (sd/wrap-check-valid-meta-key-new :id)
                        (wwrap-find-meta_key :id :id true)]
           :handler handle_adm-get-meta-key
           :coercion reitit.coercion.schema/coercion

           :swagger {:produces "application/json"
                     :parameters [{:name "id"
                                   :in "path"
                                   :description "e.g.: madek_core:subtitle"
                                   :type "string"
                                   :required true
                                   :pattern "^[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+$"}]}

           :responses {200 {:body schema_export-meta-key-adm}
                       404 {:body {:message s/Str}}
                       422 {:description "Wrong format"
                            :schema s/Str
                            :examples {"application/json" {:message "Wrong meta_key_id format! See documentation. (fdas)"}}}}}

     :put {:summary (sd/sum_adm (t "Update meta-key."))
           :handler handle_update_meta-key
           :content-type "application/json"
           :accept "application/json"

           :description (slurp "./md/meta-key-put.md")

           :middleware [wrap-authorize-admin!
                        (sd/wrap-check-valid-meta-key-new :id)
                        (wwrap-find-meta_key :id :id true)]
           :coercion reitit.coercion.schema/coercion

           :swagger {:produces "application/json"
                     :consumes "application/json"
                     :parameters [{:name "id"
                                   :in "path"
                                   :description "e.g.: copyright:test_me_now22"
                                   :type "string"
                                   :required true}]}

           :parameters {:body schema_update-meta-key}

           :responses {200 {:body schema_export-meta-key-adm}
                       406 {:description "Update failed"
                            :schema s/Str
                            :examples {"application/json" {:message "Could not update meta_key."}}}}}

     :delete {:summary (sd/sum_adm (t "Delete meta-key."))
              :handler handle_delete_meta-key
              :middleware [(sd/wrap-check-valid-meta-key-new :id)
                           (wwrap-find-meta_key :id :id true)]
              :coercion reitit.coercion.schema/coercion
              :swagger {:produces "application/json"
                        :consumes "application/json"
                        :parameters [{:name "id"
                                      :in "path"
                                      :description "e.g.: copyright:test_me_now22"
                                      :type "string"
                                      :required true}]}

              :responses {200 {:body schema_export-meta-key-adm}
                          406 {:description "Entry not found"
                               :schema s/Str
                               :examples {"application/json" {:message "No such entity in :meta_keys as :id with copyright:test_me_now22"}}}
                          ;:examples {"application/json" {:message "Could not delete meta-key."}}}
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
           :responses {200 {:description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
                            :body {:meta-keys [schema_export-meta-key-usr]}}}}}]

   ["/:id"
    {:get {:summary (sd/sum_usr_pub (v (t "Get meta-key by id")))
           :description "Get meta-key by id. Returns 404, if no such meta-key exists."
           :content-type "application/json"
           :accept "application/json"
           :handler handle_usr-get-meta-key
           :middleware [(sd/wrap-check-valid-meta-key-new :id)
                        (wwrap-find-meta_key :id :id true)]
           :coercion reitit.coercion.schema/coercion

           :swagger {:produces "application/json"
                     :parameters [{:name "id"
                                   :in "path"
                                   :description "e.g.: madek_core:subtitle"
                                   :type "string"
                                   :required true
                                   :pattern "^[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+$"}]}

           :responses {200 {:body schema_export-meta-key-usr}

                       404 {:description "No entry found for the given id"
                            :schema s/Str
                            :examples {"application/json" {:message "No such entity in :meta_keys as :id with not-existing:key"}}}

                       422 {:description "Wrong format"
                            :schema s/Str
                            :examples {"application/json" {:message "Wrong meta_key_id format! See documentation. (fdas)"}}}}}}]])

;### Debug ####################################################################
(debug/debug-ns *ns*)
