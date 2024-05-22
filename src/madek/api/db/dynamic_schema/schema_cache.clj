(ns madek.api.db.dynamic_schema.schema_cache
  (:require
   ;; all needed imports
   ;[leihs.core.db :as db]

   [madek.api.db.dynamic_schema.core :refer [create-dynamic-schema
                                             init-enums-by-db
                                             ;convert-to-enum-spec
                                             ]]

   [madek.api.db.dynamic_schema.schema_definitions :as d]

   [madek.api.db.dynamic_schema.common :refer [get-enum set-enum set-schema get-schema]]


   ;[madek.api.utils.helper :refer [merge-query-parts to-uuids]]))
   ;(def type-mapping {"varchar" s/Str
   ;                   "int4" s/Int
   ;                   "integer" s/Int
   ;                   "boolean" s/Bool
   ;                   "uuid" s/Uuid
   ;                   "text" s/Str
   ;                   "jsonb" s/Any
   ;                   "character varying" s/Str
   ;                   "timestamp with time zone" s/Any
   ;                   ;; helper
   ;                   "str" s/Str
   ;                   "any" s/Any
   ;                   }
   ))








;
;(defn type-mapping-enums [key] "Maps a <table>.<key> to a Spec type, eg.: enum OR schema-definition "
;
;  (let [
;        schema-de-en {(s/optional-key :de) (s/maybe s/Str)
;                      (s/optional-key :en) (s/maybe s/Str)}
;
;        schema-subtype (s/enum "Person" "PeopleGroup" "PeopleInstitutionalGroup")
;
;        schema-meta_datum (s/enum "MetaDatum::Text"
;                            "MetaDatum::TextDate"
;                            "MetaDatum::JSON"
;                            "MetaDatum::Keywords"
;                            "MetaDatum::People"
;                            "MetaDatum::Roles")
;        schema-allowed_people_subtypes (s/enum "People" "PeopleGroup")
;
;        schema-scope (s/enum "view" "use")
;
;        p (println ">o> !!1 type-mapping-enums.key=" key (class key))
;        enum-map {"collections.default_resource_type" (get-enum :collections_default_resource_type)
;                  "collections.layout" (get-enum :collections_layout)
;                  "collections.sorting" (get-enum :collections_sorting)
;
;                  "groups.type" (get-enum :groups.type)
;
;                  "users.settings" vector-or-hashmap-validation
;
;                  "app_settings.about_pages" schema-de-en
;                  "app_settings.brand_texts" schema-de-en
;                  "app_settings.catalog_subtitles" schema-de-en
;                  "app_settings.catalog_titles" schema-de-en
;                  "app_settings.featured_set_subtitles" schema-de-en
;                  "app_settings.featured_set_titles" schema-de-en
;                  "app_settings.provenance_notices" schema-de-en
;                  "app_settings.site_titles" schema-de-en
;                  "app_settings.support_urls" schema-de-en
;                  "app_settings.welcome_texts" schema-de-en
;                  "app_settings.welcome_titles" schema-de-en
;
;                  "app_settings.available_locales" [s/Str]
;                  "app_settings.contexts_for_entry_extra" [s/Str]
;                  "app_settings.contexts_for_entry_validation" [s/Str]
;                  "app_settings.catalog_context_keys" [s/Str]
;                  "app_settings.contexts_for_dynamic_filters" [s/Str]
;                  "app_settings.contexts_for_collection_extra" [s/Str]
;                  "app_settings.copyright_notice_templates" [s/Str]
;                  "app_settings.contexts_for_entry_edit" [s/Str]
;                  "app_settings.contexts_for_collection_edit" [s/Str]
;                  "app_settings.contexts_for_list_details" [s/Str]
;
;                  "context_keys.labels" schema-de-en
;                  "context_keys.descriptions" schema-de-en
;                  "context_keys.hints" schema-de-en
;                  "context_keys.documentation_urls" schema-de-en
;
;                  "contexts.labels" schema-de-en
;                  "contexts.descriptions" schema-de-en
;
;                  "vocabularies.descriptions" schema-de-en
;                  "vocabularies.labels" schema-de-en
;
;                  "static-pages.contents" schema-de-en
;
;                  "roles.labels" schema-de-en
;
;                  "people.external_uris" [s/Str]
;                  "people.subtype" schema-subtype
;                  "keywords.external_uris" [s/Str]
;                  ;"keywords.external_uris" [s/Any]
;
;                  ;; TODO: check if this is correct, should be "meta_keys .."
;                  "meta-keys.meta_datum_object_type" schema-meta_datum
;                  "meta-keys.allowed_people_subtypes" schema-allowed_people_subtypes
;                  "meta-keys.scope" schema-scope
;                  ;"meta_keys.meta_datum_object_type" schema-meta_datum
;                  ;"meta_keys.allowed_people_subtypes" schema-allowed_people_subtypes
;                  ;"meta_keys.scope" schema-scope
;
;                  "meta_keys.labels" schema-de-en
;                  "meta_keys.descriptions" schema-de-en
;                  "meta_keys.hints" schema-de-en
;                  "meta_keys.documentation_urls" schema-de-en
;
;                  "media_files.conversion_profiles" [s/Any]
;
;                  }
;
;        ;p (println ">o> akey=" key)
;        p (println ">o> akey=" key (class key))
;        ;p (println ">o> akeys=" (keys enum-map))
;
;        res (get enum-map key nil)
;
;        p (println ">o> !!1 res=" res)
;        ]
;    res))
;
;(def schema_full_data_raw [{:column_name "full_data", :data_type "boolean"}])
;(def schema_pagination_raw [{:column_name "page", :data_type "int4"}
;                            {:column_name "count", :data_type "int4"}])
;
;
;












(comment
  (let [
        my-type [{:id {:key-type nil}} {:created_by_user_id {:value-type "maybe"}} {:institutional_id {:value-type "maybe"}}
                 {:institutional_name {:value-type "maybe"}} {:institution {:value-type "maybe"}}]

        res (get-in my-type [:id :key-type])


        ;res (fetch-value-by-key types :id)
        ]
    res
    )

  )














;
;SELECT enumlabel
;FROM pg_enum
;JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
;WHERE pg_type.typname = 'collection_sorting';



;SELECT enumlabel
;FROM pg_enum
;JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
;WHERE pg_type.typname = 'collection_sorting';

;(defn fetch-enum [enum-name]
;
;  (println ">o> fetch-enum by DB!!!!!!!!")
;
;  (let [ds (get-ds)
;
;        ;;; TODO: FIXME: use get-ds
;        ;ds {:dbtype "postgresql"
;        ;              :dbname "madek_test"
;        ;              :user "madek_sql"
;        ;              :port 5415
;        ;              :password "madek_sql"}
;        ]
;    (try (jdbc/execute! ds
;           (-> (sql/select :enumlabel)
;               (sql/from :pg_enum)
;               (sql/join :pg_type [:= :pg_enum.enumtypid :pg_type.oid])
;               (sql/where [:= :pg_type.typname enum-name])
;               sql-format))
;
;         (catch Exception e
;           (println ">o> ERROR: fetch-table-metadata" (.getMessage e))
;           (throw (Exception. "Unable to establish a database connection"))))))


;(defn create-enum-spec
;  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
;  [enum-data]
;  ;(apply s/enum (mapv #(-> % :pg_enum :enumlabel) enum-data)))
;  (apply s/enum (mapv #(-> % :pg_enum/enumlabel) enum-data)))
;
;
;(defn create-enum-spec
;  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
;  [enum-data]
;  (let [enum-labels (mapv #(-> % :pg_enum :enumlabel) enum-data)
;        filtered-enum-labels (remove nil? enum-labels)]
;    (apply s/enum filtered-enum-labels)))




;(comment
;  (let [
;        res (create-enum-spec "collection_sorting")
;        p (println ">o> 1res=" res)
;        res (create-enum-spec "collection_layout")
;        p (println ">o> 1res=" res)
;        res (create-enum-spec "collection_default_resource_type")
;        p (println ">o> 1res=" res)
;
;        ;res (fetch-enum "collection_sorting")
;        ;p (println ">o> 1res=" res)
;        ;res (fetch-enum "collection_layout")
;        ;p (println ">o> 1res=" res)
;        ;res (fetch-enum "collection_default_resource_type")
;        ;p (println ">o> 1res=" res)
;
;
;
;
;        ;p (println ">o> 1??=" (:enumlabel (first res)))
;        ;p (println ">o> 2??=" (class (:enumlabel (:pg_enum (first res)))))
;        ;p (println ">o> 3??=" (first res))
;        ;p (println ">o> 3??=" (:pg_enum (first res)))
;        ;p (println ">o> 3??=" (:enumlabel (:pg_enum (first res))))
;        ;p (println ">o> 3??=" (:pg_enum/enumlabel (first res)))
;        ;
;        ;;[#:pg_enum{:enumlabel "created_at ASC"}
;        ;
;        ;res (create-enum-spec res)
;        ;
;        ;
;        ;p (println ">o> 2res=" res)
;
;        ]
;    res
;    )
;  )




;te_pr (println ">o> 11??=" (get-enum :collections_sorting))
;te_pr (println ">o> 11??=" (get-enum :collections_layout))
;;te_pr (println ">o> 11??=" (get-enum :collections_default_resource_type))



(defn set-schema-by-array [schema-map]


  (map (fn [[k v]]
         (set-schema k v)
    schema-map))

  ;(let [(doseq [c schema-array]
  ;        (let [key (:key c)
  ;              value (:value c)]
  ;          (set-schema key value)))
  ;  ] )
  )





(defn init-schema-by-db []
  (let [
        x (init-enums-by-db)


        ;merged-vec (into [] (concat (d/create-groups-schema) [{}]))

        _ (create-dynamic-schema d/create-groups-schema)
        _ (create-dynamic-schema d/create-users-schema)

        _ (create-dynamic-schema d/create-admins-schema)
        _ (create-dynamic-schema d/create-workflows-schema)
        _ (create-dynamic-schema d/create-collections-schema)
        _ (create-dynamic-schema d/create-collection-media-entry-schema)
        _ (create-dynamic-schema d/create-collection-collection-arcs-schema)
        _ (create-dynamic-schema d/create-app-settings-schema)
        _ (create-dynamic-schema d/create-confidential-links-schema)
        _ (create-dynamic-schema d/create-context-keys-schema)
        _ (create-dynamic-schema d/create-context-schema)
        _ (create-dynamic-schema d/create-custom-urls-schema)
        _ (create-dynamic-schema d/create-delegation-schema)

        _ (create-dynamic-schema d/create-edit_session-schema)

        _ (create-dynamic-schema d/create-vocabularies-schema)
        ;_ (create-vocabularies-schema2)
        ;_  (doseq [c d/create-vocabularies-schema2]
        ;     (let [key (:key c)
        ;           value (:value c)]
        ;       (set-schema key value)))

        _ (set-schema-by-array [d/create-vocabularies-schema2])




        _ (create-dynamic-schema d/create-usage_terms-schema)
        _ (create-dynamic-schema d/create-static_pages-schema)
        _ (create-dynamic-schema d/create-roles-schema)
        _ (create-dynamic-schema d/create-previews-schema)

        _ (create-dynamic-schema d/create-permissions-schema)
        _ (set-schema-by-array [d/top-level-permissions-schema])

        _ (create-dynamic-schema d/create-people-schema)
        _ (create-dynamic-schema d/create-keywords-schema)

        _ (create-dynamic-schema d/create-meta_keys-schema)



        _ (create-dynamic-schema d/create-media_entries-schema)
        _ (set-schema-by-array [d/top-level-media_entries-schema])

        _ (create-dynamic-schema d/create-delegations_users-schema)
        _ (create-dynamic-schema d/create-io_interfaces-schema)
        _ (create-dynamic-schema d/create-media-files-schema)
        _ (create-dynamic-schema d/create-meta-data-schema)
        _ (create-dynamic-schema d/create-meta-data-role-schema)
        _ (create-dynamic-schema d/create-favorite-media-entries-schema)



        ]))


;;; Example to save/fetch schema-configuration ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(comment
;  (let [
;        data {
;              :raw [
;                    {:groups {}}
;                    ;{:_additional (concat schema_pagination_raw schema_full_data_raw)}
;                    ]
;              :raw-schema-name :groups-schema-with-pagination-raw
;              :schemas [
;                        {:groups.schema-query-groups {:key-types "optional" :alias "schema_query-groups"}}
;                        ]
;              }
;
;        tx (get-ds)
;
;
;        data [:cast (json/generate-string data) :jsonb]
;        ins-data {:id "abc" :key "test-me" :config data}
;        query (-> (sql/insert-into :schema_definition)
;                  (sql/values [ins-data])
;                  (sql/returning :*)
;                  sql-format
;                  )
;
;        p (println ">o> query=" query)
;
;        res (jdbc/execute! tx query)
;
;
;        p (println "\nquery" res)
;
;        ]
;    res
;    )
;  )



;(comment
;  (let [
;        tx (get-ds)
;
;        query (-> (sql/select :*)
;                  (sql/from :schema_definition)
;                  (sql/where [:= :key "test-me"])
;                  sql-format
;                  )
;
;        p (println ">o> query=" query)
;
;        res (jdbc/execute-one! tx query)
;        p (println ">o> res1=" res)
;        ]
;    res
;    )
;  )

