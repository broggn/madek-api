(ns madek.api.schema_cache
  (:require
   ;; all needed imports
   ;[leihs.core.db :as db]

   [clojure.string :as str]

   [cheshire.core :as json]
   [madek.api.db.core :refer [get-ds]]



   [madek.api.resources.shared :as sd]





   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [madek.api.db.core :refer [get-ds]]
   [madek.api.utils.validation :refer [vector-or-hashmap-validation]]



   [next.jdbc :as jdbc]



   ;[madek.api.utils.helper :refer [merge-query-parts to-uuids]]

   [schema.core :as s]
   ; [taoensso.timbre :as timbre]
   ; [clojure.core.cache :as cache]
   [taoensso.timbre :refer [spy]])
  )

(defn pr [key fnc]
  (println ">oo> HELPER / " key "=" fnc)
  fnc
  )


(def enum-cache (atom {}))
(def schema-cache (atom {}))





(def TYPE_NOTHING "nothing")
(def TYPE_REQUIRED "required")
(def TYPE_OPTIONAL "optional")
(def TYPE_MAYBE "maybe")


(require '[schema.core :as schema])

(def type-mapping {"varchar" s/Str
                   "int4" s/Int
                   "integer" s/Int
                   "boolean" s/Bool
                   "uuid" s/Uuid
                   "text" s/Str
                   "jsonb" s/Any
                   "character varying" s/Str
                   "timestamp with time zone" s/Any
                   ;; helper
                   "str" s/Str
                   }
  )



(defn get-enum [key & [default]]

  (let [
        val (get @enum-cache key default)
        p (println ">o> key=" key)
        p (println ">o> val=" val)
        p (println ">o> default=" default)

        ] val)

  ;(println ">oo> get-enum.key=" key)
  ;(pr key (get @enum-cache key default))
  )


(defn set-enum [key value]
  (println ">oo> set-enum.key=" key)
  (swap! enum-cache assoc key value))


(defn get-schema [key & [default]]
  (let [
        val (get @schema-cache key default)
        ;val2 (get @schema-cache (name key) default)
        ;p (println ">o>s get-schema.key=" key)
        ;p (println ">o>s get-schema.val=" val)
        ;p (println ">o>s val2=" val2)

        ;_ (if (nil? val) (System/exit 0 ))
        val (if (nil? val)
              (do
                ;(println ">o> CAUTION !!!!! no-schema-found!!!" key)
                s/Any)
              ;(s/Any)
              ;s/Any

              ;val)
              (into {} val)
              )



        p (println ">o> [get-schema] " key "=" val)

        ] val)
  )



(defn get-schema-converted [key]
  (let [
        res (get-schema key)
        ;res (into {} res)

        ;p (println ">o> get-schema-by-name.key=" key)
        ;p (println ">o> get-schema-by-name.val=" val)
        ] res)
  )




(defn set-schema [key value]
  (let [
        p (println ">o> !!! [set-schema] (" key ") ->" value)

        value (into {} value)

        res (swap! schema-cache assoc key value)
        ] res)
  )


(defn type-mapping-enums [key] "Maps a <table>.<key> to a Spec type, eg.: enum OR schema-definition "

  (let [
        schema-de-en {(s/optional-key :de) (s/maybe s/Str)
                      (s/optional-key :en) (s/maybe s/Str)}


        p (println ">o> !!1 type-mapping-enums.key=" key (class key))
        enum-map {"collections.default_resource_type" (get-enum :collections_default_resource_type)
                  "collections.layout" (get-enum :collections_layout)
                  "collections.sorting" (get-enum :collections_sorting)


                  "groups.type" (get-enum :groups.type)

                  "users.settings" vector-or-hashmap-validation
                  ;"users.settings" s/Any


                  "app_settings.about_pages" schema-de-en
                  "app_settings.brand_texts" schema-de-en
                  "app_settings.catalog_subtitles" schema-de-en
                  "app_settings.catalog_titles" schema-de-en
                  "app_settings.featured_set_subtitles" schema-de-en
                  "app_settings.featured_set_titles" schema-de-en
                  "app_settings.provenance_notices" schema-de-en
                  "app_settings.site_titles" schema-de-en
                  "app_settings.support_urls" schema-de-en
                  "app_settings.welcome_texts" schema-de-en
                  "app_settings.welcome_titles" schema-de-en


                  "context_keys.labels" schema-de-en
                  "context_keys.descriptions" schema-de-en
                  "context_keys.hints" schema-de-en
                  "context_keys.documentation_urls" schema-de-en

                  "contexts.labels" schema-de-en
                  "contexts.descriptions" schema-de-en
                  ;"get.body.my-enum" schema-de-en


                  ;"groups.id" "uuid"

                  }

        ;p (println ">o> akey=" key)
        p (println ">o> akey=" key (class key))
        ;p (println ">o> akeys=" (keys enum-map))

        res (get enum-map key nil)

        p (println ">o> !!1 res=" res)
        ]
    res))

;(def schema_full_data_raw [{:column_name "full_data", :data_type "boolean" :key-type TYPE_OPTIONAL}])
;(def schema_pagination_raw [{:column_name "page", :data_type "int4" :key-type TYPE_OPTIONAL}
;                            {:column_name "count", :data_type "int4" :key-type TYPE_OPTIONAL}])

(def schema_full_data_raw [{:column_name "full_data", :data_type "boolean"}])
(def schema_pagination_raw [{:column_name "page", :data_type "int4"}
                            {:column_name "count", :data_type "int4"}])










(defn fetch-table-metadata [table-name]
  (println ">o> fetch-table-metadata by DB!!!!!!!!" table-name)
  (let [ds (get-ds)]
    (try (jdbc/execute! ds
           ;(-> (sql/select :column_name :data_type :is_nullable)
           (-> (sql/select :column_name :data_type)
               (sql/from :information_schema.columns)
               (sql/where [:= :table_name table-name])
               sql-format
               spy
               ))
         (catch Exception e
           (println ">o> ERROR: fetch-table-metadata" (.getMessage e))
           (throw (Exception. "Unable to establish a database connection"))))))












(defn remove-by-keys [data keys-to-remove]

  (let [
        ;data (when-not (nil? (:rename data)) (rename-by-keys data (:rename data)))
        ;p (println ">o> >>> mod.data=" data)
        ;_ (System/exit 0)
        res (remove (fn [[key _]]
                      (some #{key} keys-to-remove))
              data)

        ] res))


(defn keep-by-keys [data keys-to-keep]
  (let [
        ;data (when-not (nil? (:rename data)) (rename-by-keys data (:rename data)))
        res (filter (fn [[key _]]
                      (some #{key} keys-to-keep))
              data)

        ] res))


(defn fetch-value-by-key
  [maps key]
  (some (fn [m] (get m key)) maps))








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





(defn revise-schema-types [table-name column_name column_type type-spec types key-types value-types]

  (let [

        p (println "\n>o> [revise-schema-types] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n
        table-name=" table-name "\n
        column_name=" column_name "\n
        column_type=" column_type "\n
        type-spec=" type-spec "\n
        types=" types "\n
        keys-types=" key-types "\n
        value-types=" value-types "\n
        >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        ")


        ;;key-type TYPE_OPTIONAL
        ;;value-type TYPE_MAYBE
        ;
        ;p (println "--------------\nColumn Name (column_name):" column_name)
        ;p (println "Column Type (column_type):" column_type)
        ;p (println "Type Specification (type-spec):" type-spec)
        ;p (println "types:" types)
        ;
        ;data_type column_type
        ;
        ;keySection nil
        ;valueSection nil


        ;p (println ">o> ----------------- HEREEEEE")
        ;p (println ">o> revise-schema-types.types=" types)
        ;p (println ">o> revise-schema-types.key-types=" key-types)
        ;p (println ">o> revise-schema-types.value-types=" value-types)
        ;p (println ">o> val=" val)


        ;p (println "\n>o> [revise-schema-types] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        ;p (println ">o> type-spec=" type-spec)
        ;p (println ">o> types=" types)
        ;p (println ">o> key-types=" key-types)
        ;p (println ">o> value-types=" value-types)

        ;; overwrite schema types, use :key-types as default and overwrite it by specific :key-type
        val (fetch-value-by-key types column_name)

        key-type (get val :key-type)
        value-type (get val :value-type)
        ;key-type (:key-type val)
        ;value-type (:value-type val)


        ;key-type (get-in types [(keyword column_name) :key-type])
        ;value-type (get-in types [(keyword column_name) :value-type])




        ;p (println ">o> val=" val)
        p (println ">o> >>>>>>>> val=" val)
        p (println ">o> >>>>>>>> key-type=" key-type)
        ;p (println ">o> value-type=" value-type)

        key-type (if (not (nil? key-types))
                   (if (nil? key-type) key-types key-type)
                   key-type)

        value-type (if (not (nil? value-types))
                     ;value-types
                     (if (nil? value-type) value-types value-type)
                     value-type)



        ;p (println ">o> final.key-type=" key-type)
        ;p (println ">o> final.value-type=" value-type)
        ;p (println ">o> [revise-schema-types] <<<<<<<<<<<<<<< before <<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n
        ;type-mapping-key=" type-mapping-key "\n
        ;column_name=" column_name "\n
        ;key-type=" key-type "\n
        ;value-type=" value-type "\n
        ;")

        keySection (cond (= key-type TYPE_REQUIRED) (s/required-key (keyword column_name))
                         (= key-type TYPE_OPTIONAL) (s/optional-key (keyword column_name))
                         (= key-type TYPE_NOTHING) (keyword column_name)
                         :else (do
                                 ;(println ">o> nix")
                                 (keyword column_name))
                         )

        ;; revise schema types by mapping
        type-mapping-key (str (name table-name) "." (name column_name))
        type-mapping-enums-res (type-mapping-enums type-mapping-key)
        ;p (println ">o> ?????????? type-mapping-key => " type-mapping-key "/" type-mapping-enums-res)
        ;
        ;p (println ">o> ?????????? value-type=" value-type)
        valueSection (cond
                       (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                             (s/maybe type-mapping-enums-res)
                                                             type-mapping-enums-res)
                       (= value-type TYPE_MAYBE) (s/maybe column_type)
                       :else column_type
                       )

        ;p (println ">o> revise-schema-types, final-result  (" key ")=>> " {keySection valueSection})

        p (println ">o> [revise-schema-types] <<<<<<<<<<<<<<< before <<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n
        type-mapping-key=" type-mapping-key "\n
        column_name=" column_name "\n
        key-type=" key-type "\n
        value-type=" value-type "\n
        ")

        p (println ">>o> !!! [set-schema], ?? final-result  (" (table-name "." column_name) ")  (" key ")=>> " {keySection valueSection})


        ;p (if (= (table-name "." column_name) :groups.type)
        ;   (throw (Exception. "groups.type INFO????????????")))

        ]

    {keySection valueSection}
    ))


(defn process-revision-of-schema-types
  [table-name list-of-maps types key-types value-types]
  (map
    (fn [[col-name col-type]]
      (let [type-spec (some (fn [type-map]
                              (get type-map col-name))
                        types)]
        (revise-schema-types table-name col-name col-type type-spec types key-types value-types)))
    list-of-maps))


(defn create-schemas-by-config [data]
  (let [
        schema-def (:schemas data)
        raw-schema-name (:raw-schema-name data)

        _ (if (nil? schema-def)
            (throw (Exception. "[create-schemas-by-config()] No data.schemas definition found")))

        _ (if (nil? raw-schema-name)
            (throw (Exception. "[create-schemas-by-config()] No data.raw-schema-name definition found")))


        result []

        p (println ">o> (entry) 1???????? xx attr1:" schema-def)
        res (reduce
              (fn [acc item]
                (println ">o> (entry) 2???????? xx attr1:" item)
                (reduce (fn [inner-acc [key value]]
                          ;(println ">o> xx attr2b:" key)
                          ;(println ">o> xx attr2c:" value)
                          ;(println ">o> xx attr2 ??: -------------------------")

                          (let [

                                p (println ">o> xx attr2 ??: --------2-----------------")
                                schema-raw (get-schema raw-schema-name)

                                ;p (println ">o> xx attr2 ??: --------3-----------------")
                                ;p (println ">o> value=" value)
                                ;;[k v] value
                                ;p (println ">o> xx attr2 ??: --------4-----------------")
                                ;

                                table-name key
                                wl-attr (:wl value)
                                bl-attr (:bl value)

                                types-attr (:types value)
                                key-types (:key-types value)
                                value-types (:value-types value)

                                ;rename-attr (:rename value)
                                key (name key)


                                p (println ">o> X HERE WE GO\n---------------------------------")
                                p (println ">o> X table-name=" table-name)
                                p (println ">o> X wl-attr=" wl-attr)
                                p (println ">o> X bl-attr=" bl-attr)
                                p (println ">o> X types-attr=" types-attr)
                                ;p (println ">o> rename-attr=" rename-attr)
                                p (println ">o> X key=" key)
                                p (println ">o> X key-types=" key-types)
                                p (println ">o> X value-types=" value-types)


                                res schema-raw

                                res2 (cond
                                       (not (nil? bl-attr)) (remove-by-keys res bl-attr)
                                       (not (nil? wl-attr)) (keep-by-keys res wl-attr)
                                       :else res)

                                ;p (println ">o> ??? >>>>>>>> BEFORE >>>>>>>>> res2=" res2)
                                ;
                                ;p (println ">o> ----------------- HEREEEEE")
                                ;p (println ">o> revise-schema-types.key=" key)
                                ;p (println ">o> revise-schema-types.value=" value)
                                ;
                                ;p (println ">o> revise-schema-types.types=" types-attr)
                                ;p (println ">o> revise-schema-types.key-types=" key-types)
                                ;p (println ">o> revise-schema-types.value-types=" value-types)

                                res2 (process-revision-of-schema-types table-name res2 types-attr key-types value-types)
                                ;p (println ">o> ??? >>>>>>>> AFTER >>>>>>>>> res2=" res2)


                                p (println ">o> !!! [set-schema] ?" (keyword key) "/" res2)


                                _ (set-schema (keyword key) res2)
                                ;p (println ">o> >>>>>>>>> cache-schema, " key "/" res2)
                                ]

                            ;(println ">o> a?????bc" res2)

                            (into inner-acc res2)
                            )

                          )                                 ; Return inner-acc unchanged if condition is not met

                  acc item))                                ; Initialize inner-acc with acc and iterate over item
              result schema-def)                            ; Initialize reduce with an empty vector

        ] res)

  )





(defn postgres-cfg-to-schema [table-name metadata]

  (println ">oo> postgres-cfg-to-schema --------------------------------")
  (println ">oo> postgres-cfg-to-schema.table-name=" table-name)
  (println ">oo> postgres-cfg-to-schema.metadata=" metadata)
  ;(println ">oo> postgres-cfg-to-schema.metadata.keys=" (keys metadata))

  (into {}
    (map (fn [{:keys [column_name data_type key-type value-type]}]
           (println ">o> postgres-cfg-to-schema =>" table-name column_name data_type key-type value-type)

           (let [

                 ;key-type TYPE_OPTIONAL
                 ;value-type TYPE_MAYBE

                 p (println ">o> key-type" key-type)
                 p (println ">o> key-type?" (= key-type TYPE_OPTIONAL))

                 keySection (cond (= key-type TYPE_REQUIRED)
                                  (s/required-key (keyword column_name))
                                  ;(do
                                  ;                             p (println ">o> require")
                                  ;
                                  ;                             (s/required-key (keyword column_name)))

                                  (= key-type TYPE_OPTIONAL) (s/optional-key (keyword column_name))
                                  ;(= key-type TYPE_MAYBE) (s/maybe (s/optional-key (keyword column_name)))
                                  ;:else (error ">o> ERROR: key-type not supported")
                                  :else (do
                                          p (println ">o> nix")
                                          (keyword column_name))
                                  )

                 ;; FYI: <table>.<column> eg.: "groups.type"
                 type-mapping-key (str (name table-name) "." column_name)

                 type-mapping-enums-res (type-mapping-enums type-mapping-key)
                 type-mapping-res (type-mapping data_type)  ;raw-mapping


                 p (println ">o> abc?????????" type-mapping-key)

                 ;_ (if (= type-mapping-key :groups.type)
                 ;     (do
                 p (println ">o> groups.type")
                 p (println ">o> groups.type =>> type-mapping-res=" type-mapping-res "(" data_type ")")
                 p (println ">o> groups.type =>> type-mapping-enums-res=" type-mapping-enums-res)
                 ;(throw (Exception. "groups.type INFO????????????")
                 ;)
                 ;))

                 ;p (println ">o> final-result =>> type-mapping-key=" type-mapping-key)


                 valueSection (cond
                                (str/starts-with? data_type "enum::") (get-enum (keyword (str/replace data_type #"enum::" "")))

                                ;(= value-type TYPE_MAYBE) (s/maybe data_type)
                                ;:else valueSection


                                (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                                      (s/maybe type-mapping-enums-res)
                                                                      type-mapping-enums-res)


                                (not (nil? type-mapping-res)) (if (= value-type TYPE_MAYBE)
                                                                ;(s/maybe (type-mapping data_type))
                                                                ;(type-mapping data_type)

                                                                (s/maybe type-mapping-res)
                                                                type-mapping-res
                                                                )







                                ;(= value-type TYPE_MAYBE) (s/maybe (type-mapping data_type))
                                ;:else (type-mapping data_type)

                                (= value-type TYPE_MAYBE) (s/maybe s/Any)
                                :else s/Any
                                )


                 p (println ">o> [postgres-cfg-to-schema] table= " table-name ", final-result =>> " {keySection valueSection})
                 p (println ">oo>  --------------------------------")


                 ;_ (System/exit 0)

                 ]
             {keySection valueSection}
             ;nil
             ))

      metadata)))


(defn remove-maps-by-entry-values
  "Removes maps from a list where the specified entry key matches any of the values in the provided list."

  ([maps target-values]
   (remove-maps-by-entry-values maps :column_name target-values)
   )

  ;; TODO: fix this to handle [:foo :bar]
  ([maps entry-key target-values]

   (if (empty? target-values)
     maps
     ; else
     (remove #(some #{(entry-key %)} target-values) maps)))

  )


(defn keep-maps-by-entry-values
  "Keeps only maps from a list where the specified entry key matches any of the values in the provided list."


  ([maps target-values]
   (keep-maps-by-entry-values maps :column_name target-values)
   )

  ;; TODO: fix this to handle [:foo :bar]
  ([maps entry-key target-values]

   (if (empty? target-values)
     maps
     ; else
     (filter #(some #{(entry-key %)} target-values) maps)))

  )




(defn rename-column-names
  [maps key-map]


  (println ">o> >>??>> X key-map=" key-map)
  (println ">o> >>??>> X maps=" maps)

  (map
    (fn [m]
      (if-let [new-col-name (get key-map (:column_name m))]
        (assoc m :column_name new-col-name)
        m))
    maps))

(defn create-raw-schema [data]
  (let [raw (data :raw)
        raw-schema-name (data :raw-schema-name)
        result []

        res (reduce
              (fn [acc item]
                (println ">o> (entry) attr1:" item)
                (reduce (fn [inner-acc [key value]]

                          ;(println ">o> attr2b:" key)
                          ;(println ">o> attr2c:" value)
                          ;(println ">o> attr2 ??: -------------------------")

                          (cond
                            (not (str/starts-with? (name key) "_"))
                            (let [p (println ">o> [handle not-additional]")

                                  table-name key
                                  wl-attr (:wl value)
                                  bl-attr (:bl value)

                                  rename-attr (:rename value)
                                  ;p (println ">o> >>??>> (" key ") raw.rename-attr=" rename-attr)



                                  key (name key)

                                  ;p (println ">o> table-name1 >> " (class key))
                                  ;p (println ">o> table-name2 >> " key)

                                  res (fetch-table-metadata key)
                                  ;p (println ">o> >>??>> (" key ") raw.res=" res)

                                  p (println ">o> [handle not-additional] \n
                                  table-name=" table-name "\n
                                  wl-attr=" wl-attr "\n
                                  bl-attr=" bl-attr "\n
                                  rename-attr=" rename-attr "\n
                                  key=" key "\n
                                  db-data=" res "\n")

                                  res (if (nil? rename-attr)
                                        res
                                        (rename-column-names res rename-attr))

                                  p (println "\n>o> >>??>> (" key ") raw.after-rename=" res "\n")


                                  res2 (cond
                                         (not (nil? bl-attr)) (remove-maps-by-entry-values res :column_name bl-attr)
                                         (not (nil? wl-attr)) (keep-maps-by-entry-values res :column_name wl-attr)
                                         :else res)

                                  ;res2 (raw-type-mapping table-name res2)


                                  p (println ">o> [handle not-additional]
                                  table-name=" table-name "\n
                                  res2=" res2 "\n")


                                  res3 (postgres-cfg-to-schema table-name res2)

                                  ]

                              (println "\n>o> res3=" res3 "\n")
                              (println ">o> -------------------------------------")

                              (into inner-acc res3))        ; Concat res2 into inner-acc

                            (= (name key) "_additional")
                            (let [p (println ">o> [handle _additional]")

                                  table-name key

                                  res2 value


                                  ;res2 (raw-type-mapping table-name res2)

                                  res2 (postgres-cfg-to-schema table-name res2)

                                  ]

                              (println "\n>o> res2=" res2 "\n")
                              (println ">o> -------------------------------------")

                              (into inner-acc res2))        ; Concat res2 into inner-acc

                            :else inner-acc))               ; Return inner-acc unchanged if condition is not met

                  acc item))                                ; Initialize inner-acc with acc and iterate over item
              result raw)                                   ; Initialize reduce with an empty vector


        _ (set-schema raw-schema-name res)

        ;_ (println ">o> final          res=" res)
        ;_ (println ">o> LOADED / final res=" (get-schema raw-schema-name))
        ;_ (println ">o> und tschüss")
        ;_ (System/exit 0)
        ;
        ;; Further processing if needed
        ]

    res))                                                   ; Return the final accumulated result










;
;SELECT enumlabel
;FROM pg_enum
;JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
;WHERE pg_type.typname = 'collection_sorting';



;SELECT enumlabel
;FROM pg_enum
;JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
;WHERE pg_type.typname = 'collection_sorting';

(defn fetch-enum [enum-name]

  (println ">o> fetch-enum by DB!!!!!!!!")

  (let [ds (get-ds)

        ;;; TODO: FIXME: use get-ds
        ;ds {:dbtype "postgresql"
        ;              :dbname "madek_test"
        ;              :user "madek_sql"
        ;              :port 5415
        ;              :password "madek_sql"}
        ]
    (try (jdbc/execute! ds
           (-> (sql/select :enumlabel)
               (sql/from :pg_enum)
               (sql/join :pg_type [:= :pg_enum.enumtypid :pg_type.oid])
               (sql/where [:= :pg_type.typname enum-name])
               sql-format))

         (catch Exception e
           (println ">o> ERROR: fetch-table-metadata" (.getMessage e))
           (throw (Exception. "Unable to establish a database connection"))))))


(defn create-enum-spec
  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
  [enum-data]
  ;(apply s/enum (mapv #(-> % :pg_enum :enumlabel) enum-data)))
  (apply s/enum (mapv #(-> % :pg_enum/enumlabel) enum-data)))


(defn create-enum-spec
  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
  [enum-data]
  (let [enum-labels (mapv #(-> % :pg_enum :enumlabel) enum-data)
        filtered-enum-labels (remove nil? enum-labels)]
    (apply s/enum filtered-enum-labels)))


(defn convert-to-enum-spec
  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
  [enum-data]

  (println ">o> enum-data=" enum-data)

  (apply s/enum (mapv #(:enumlabel %) enum-data)))
;(apply s/enum (mapv #(:pg_enum/enumlabel %) enum-data)))


(defn create-enum-spec [table-name]


  (let [
        res (fetch-enum table-name)
        p (println ">o> 1ares=" res)
        res (convert-to-enum-spec res)
        p (println ">o> 2ares=" res)

        ] res)

  ;(convert-to-enum-spec (fetch-enum table-name))
  )


(comment
  (let [
        res (create-enum-spec "collection_sorting")
        p (println ">o> 1res=" res)
        res (create-enum-spec "collection_layout")
        p (println ">o> 1res=" res)
        res (create-enum-spec "collection_default_resource_type")
        p (println ">o> 1res=" res)

        ;res (fetch-enum "collection_sorting")
        ;p (println ">o> 1res=" res)
        ;res (fetch-enum "collection_layout")
        ;p (println ">o> 1res=" res)
        ;res (fetch-enum "collection_default_resource_type")
        ;p (println ">o> 1res=" res)




        ;p (println ">o> 1??=" (:enumlabel (first res)))
        ;p (println ">o> 2??=" (class (:enumlabel (:pg_enum (first res)))))
        ;p (println ">o> 3??=" (first res))
        ;p (println ">o> 3??=" (:pg_enum (first res)))
        ;p (println ">o> 3??=" (:enumlabel (:pg_enum (first res))))
        ;p (println ">o> 3??=" (:pg_enum/enumlabel (first res)))
        ;
        ;;[#:pg_enum{:enumlabel "created_at ASC"}
        ;
        ;res (create-enum-spec res)
        ;
        ;
        ;p (println ">o> 2res=" res)

        ]
    res
    )
  )




;te_pr (println ">o> 11??=" (get-enum :collections_sorting))
;te_pr (println ">o> 11??=" (get-enum :collections_layout))
;;te_pr (println ">o> 11??=" (get-enum :collections_default_resource_type))


(defn transform-column [row table-name enum-map]
  (let [
        mkey (keyword (str table-name "." (row :column_name)))
        new-data-type (get enum-map mkey)

        ;p (println ">o> mkey=" mkey)
        ;p (println ">o> new-data-type=" new-data-type)
        ]
    (if new-data-type
      (assoc row :data_type new-data-type)
      row)))

(defn transform-schema [data table-name enum-map]
  (map #(transform-column % table-name enum-map) data))


(defn raw-type-mapping [table-name metadata]

  (let [

        raw-type-mapping {
                          ;:delegations.test "enum::test"
                          ;:groups.id "enum::what-the-fuck"
                          :groups.type "enum::groups.type"

                          }

        res metadata

        p (println ">o> !!!!!!1 res=" res)

        ;; FYI: modify raw-type if raw-type-mapping-mapping is found
        res (transform-schema metadata table-name raw-type-mapping)

        p (println ">o> !!!!!!2 res=" res)

        ;p (System/exit 0)

        ]

    res
    )
  )




(defn fetch-column-names
  "Extracts the values of :column_name from a list of maps."
  [maps]
  (map :column_name maps))

(defn replace-elem
  "Replaces an element in the vector of maps with a new map where the key matches."
  [data new-list-of-maps key]
  (mapv (fn [item]
          (println ">o> item=" item)
          (if-let [replacement (first (filter #(= (key %) (key item)) new-list-of-maps))]
            replacement
            item))
    data))


(defn fetch-table-meta-raw

  ([table-name]
   (fetch-table-meta-raw table-name [])
   )

  ([table-name update-data]
   (let [
         res (fetch-table-metadata table-name)
         p (println ">o> 1res=" res)


         res (concat res update-data)

         ;res (type-mapping table-name res)
         res (raw-type-mapping table-name res)

         ;res (map normalize-map res)
         ;p (println ">o> 2res=" res)


         ;res (replace-elem res update-data :column_name)    ;;??

         ] res))

  )






(defn create-schema-by-data
  ([table-name table-meta-raw] "Prepare schema for a table."
   (println ">o> table-name3=" table-name)
   ;(println ">o> table-name3.raw=" table-meta-raw)
   (create-schema-by-data table-name table-meta-raw [] [] [] []))

  ([table-name table-meta-raw additional-schema-list-raw] "Prepare schema for a table."
   (println ">o> table-name2=" table-name)
   (create-schema-by-data table-name table-meta-raw additional-schema-list-raw [] [] []))

  ([table-name table-meta-raw additional-schema-list-raw blacklist-key-names update-schema-list-raw whitelist-key-names] "Prepare schema for a table."
   (println ">o> table-name1=" table-name)
   (let [

         res table-meta-raw

         ; remove all entries which are not in the whitelist by column_name
         res (keep-maps-by-entry-values res :column_name whitelist-key-names)


         ;p (println ">o> debug2")
         ;res (concat res additional-schema-list-raw)        ;;FIXME
         ;p (println "\n\n>o> 3res=" res)

         res (remove-maps-by-entry-values res :column_name blacklist-key-names)
         p (println "\n\n>o> 4res=" res)
         p (println "\n\n>o> 4res.keys=" (fetch-column-names res))

         p (println ">o> debug3")
         res (replace-elem res update-schema-list-raw :column_name) ;;TODO: dont replace just update
         p (println "\n\n>o> 5res=" res)

         ;res (convert-raw-into-postgres-cfg res)
         ;p (println "\n\n>o> 6res=" res)
         ;p (println ">o> debug4")

         res (postgres-cfg-to-schema table-name res)
         p (println "\n>o> 7res=" res)

         p (println ">o> debug5")

         ] res)))



(defn create-schema-by-data-bl [table-name table-meta-raw blacklist] "Helper to create a schema for a table with blacklist."
  (println ">o> table-name3=" table-name)
  (create-schema-by-data table-name table-meta-raw [] blacklist [] []))

(defn create-schema-by-data-wl [table-name table-meta-raw whitelist] "Helper to create a schema for a table with whitelist."
  (println ">o> table-name3=" table-name)
  (create-schema-by-data table-name table-meta-raw [] [] [] whitelist))


(defn update-column-value [data column-name new-value]
  (map (fn [row]
         (if (= (row :column_name) column-name)
           (assoc row :column_name new-value)
           row))
    data))



(defn create-groups-schema []
  (let [

        ;; :groups-schema-with-pagination-raw
        ;; :groups-schema-with-pagination
        ;; :groups-schema-response
        data {
              :raw [
                    ;{:groups {:wl ["name" "id"] :rename {"name" "my-name"}}}
                    {:groups {}}
                    {:_additional (concat schema_pagination_raw schema_full_data_raw)}
                    ]
              :raw-schema-name :groups-schema-with-pagination-raw

              :schemas [
                        {:groups.schema-query-groups {:key-types "optional" :alias "schema_query-groups"}}
                        ;{:groups-schema-response-put {
                        ;                              ;:template :groups-schema-with-pagination-raw
                        ;                              :wl ["name" "type" "institution" "institutional_id" "institutional_name" "created_by_user_id"]}}
                        ]
              }
        res (create-raw-schema data)
        res2 (create-schemas-by-config data)

        ;_ (doseq [k [:groups-schema-with-pagination-raw :groups-schema-with-pagination :groups-schema-response-put]]
        ;    (try
        ;      (println ">>>> print-schema (" k "): " (get-schema-converted k)
        ;        ", get-schema-converted=" (s/checker (get-schema-converted k))
        ;        ", get-schema=" (get-schema k)
        ;        ", isValid=" (map? (get-schema-converted k))
        ;        )
        ;      ;(println ">>>> print-schema (" k "): " (get-schema k) ", isValid=" (s/checker (get-schema k)))
        ;      ;(println ">>>> print-schema (" k "): " (get-schema k) )
        ;
        ;      (catch Exception e (println ">>>> ERROR: print-schema (" k "): " (.getMessage e)))
        ;      )
        ;    )


        p (println ">o> 1create-raw-schema (" (:raw-schema-name data) ") = " res)
        p (println ">o> 2create-raw-schema (" (:raw-schema-name data) ") = " res2)

        ;_ (System/exit 0)


        ; list of keywords want to iterate
        ;(map keyword ["name" "type" "institution" "institutional_id" "institutional_name" "created_by_user_id"])




        ;; :groups-schema-raw
        ;; :groups-schema-response-put
        data {
              :raw [{:groups {}}],
              :raw-schema-name :groups-schema-raw
              :schemas [
                        ;{:groups-schema-response-put {:alias "schema_update-group"
                        ;                              :template :groups-schema-with-pagination-raw
                        ;                              :wl ["name" "type" "institution" "institutional_id" "institutional_name" "created_by_user_id"]}
                        ;                          }
                        {:groups.schema-update-group {:alias "schema_update-group"
                                                      :key-types "optional"
                                                      :value-types "maybe"
                                                      :types [{:name {:value-type TYPE_NOTHING}} {:type {:value-type TYPE_NOTHING}}]
                                                      :wl [:name :type :institution :institutional_id :institutional_name :created_by_user_id]}
                         }
                        {:groups.schema-export-group {:alias "schema_export-group"
                                                      :key-types TYPE_OPTIONAL
                                                      :types [
                                                              {:id {:key-type TYPE_NOTHING}}
                                                              {:created_by_user_id {:value-type "maybe"}}
                                                              {:institutional_id {:value-type "maybe"}}
                                                              {:institutional_name {:value-type "maybe"}}
                                                              {:institution {:value-type "maybe"}}]
                                                      }
                         }
                        {:groups.schema-import-group {:alias "schema_import-group"
                                                      :key-types TYPE_OPTIONAL
                                                      :value-types TYPE_MAYBE
                                                      :types [
                                                              ;{:id {:key-type TYPE_NOTHING}}
                                                              {:name {:key-type TYPE_NOTHING :value-type TYPE_NOTHING}}
                                                              {:type {:value-type TYPE_NOTHING}}
                                                              {:created_by_user_id {:value-type "maybe"}}
                                                              {:institutional_id {:value-type "maybe"}}
                                                              {:institutional_name {:value-type "maybe"}}
                                                              {:institution {:value-type "maybe"}}]
                                                      }
                         }
                        ]
              }
        res (create-raw-schema data)
        res2 (create-schemas-by-config data)
        p (println ">o> 3create-raw-schema (" (:raw-schema-name data) ") = " res)
        p (println ">o> 4create-raw-schema (" (:raw-schema-name data) ") = " res2)




        ;; :users-schema-raw
        data {
              :raw [{:users {}}],
              :raw-schema-name :users-schema-raw
              }
        res (create-raw-schema data)
        p (println ">o> create-raw-schema (" (:raw-schema-name data) ") = " res)




        ;; :groups-schema-response-user-simple-raw
        ;; :groups-schema-response-user-simple
        ;; :groups-schema-response-put-users
        ;; example how to extract & merge meta-data-infos (PUT "/:group-id/users/")
        data {
              :raw [
                    ;{:groups {:wl ["email" "person_id"] :_rename {"person_id" "person-id"}}}
                    ;{:users {:wl ["institutional_id" "id" "login" "created_at" "updated_at"] :_rename {"institutional_id" "institutional-id"}}}

                    ;{:groups {:wl [:email :person_id] }}
                    ;{:users {:wl [:institutional_id :id :login :created_at :updated_at] }}

                    ;{:users {:wl [:email :person_id] }}

                    ;{:users {:wl [:id :email  :institutional_id  :login :created_at :updated_at :person_id] }} ;; broken:   ;; TODO: fix this to handle [:foo :bar]
                    {:users {:wl ["id" "email" "institutional_id" "login" "created_at" "updated_at" "person_id"]}}
                    ],
              :raw-schema-name :groups-schema-response-user-simple-raw

              :schemas [
                        {:groups.schema-response-user-simple {
                                                              :alias "schema_response-user-simple"
                                                              :value-types "maybe"
                                                              :types [{:id {:value-type TYPE_NOTHING}}]
                                                              :bl [:login :created_at :updated_at]
                                                              }}

                        {:groups.schema-update-group-user-list {
                                                                :alias "schema_update-group-user-list"
                                                                :key-types "optional"
                                                                :types [{:id {:key-type TYPE_NOTHING}}]
                                                                :bl [:login :created_at :updated_at :person_id]
                                                                }}
                        ; ;; bak
                        ;{:groups-schema-response-user-simple {
                        ;                                      :alias "schema_response-user-simple"
                        ;                                      ;:template :groups-schema-response-user-simple-raw
                        ;                                      :value-types "maybe"
                        ;                                      :types [{:id {:value-type nil}}]
                        ;                                      :bl [:login :created_at :updated_at]
                        ;                                      }}

                        ;{:groups-schema-response-put-users {
                        ;                                    ;:template :groups-schema-response-user-simple-raw
                        ;                                    ;:value-types "maybe"
                        ;                                    :types [
                        ;                                            {:email {:value-type "maybe"}}
                        ;                                            ;{:institutional_id {:value-type "maybe"}}
                        ;                                            {:institutional-id {:value-type "maybe"}}
                        ;                                            {:login {:value-type "maybe"}}
                        ;                                            ]
                        ;                                    ;:bl [:login :created_at :updated_at]
                        ;                                    }}
                        ]
              }
        res (create-raw-schema data)
        res2 (create-schemas-by-config data)
        p (println ">o> 6create-raw-schema >>> (" (:raw-schema-name data) ") = " res)
        ;p (println ">o> 7create-raw-schema (" (:raw-schema-name data) ") = " res2)









        ;;; :groups-schema-response-put-users
        ;data {
        ;      :raw [
        ;            {:groups {:rename ["person_id" "person-id" "institutional_id" "institutional-id"]}}
        ;            ],
        ;      :raw-schema-name :groups-schema-response-put-users
        ;      }
        ;res (create-raw-schema data)
        ;p (println ">o> create-raw-schema (" (:raw-schema-name data) ") = " res)






        ;;; :groups-schema-raw
        ;;groups-meta-raw (fetch-table-meta-raw "groups" [{:column_name "type" :data_type "enum::groups.type" :is_nullable "NO"}])
        ;additional-schema-list-raw (concat schema_pagination_raw schema_full_data_raw)
        ;groups-meta-raw (fetch-table-meta-raw "groups" additional-schema-list-raw)
        ;_ (set-schema :groups-schema-raw groups-meta-raw)
        ;
        ;;; :users-schema-raw
        ;;users-meta-raw (fetch-table-meta-raw "users" [])
        ;users-meta-raw (fetch-table-meta-raw "users")
        ;_ (set-schema :users-schema-raw users-meta-raw)
        ;
        ;;; :groups-schema-with-pagination
        ;;additional-schema-list-raw (concat schema_pagination_raw schema_full_data_raw)
        ;p (println ">o> debug1")
        ;;res (set-schema :groups-schema-with-pagination (create-schema-by-data "groups" groups-meta-raw additional-schema-list-raw))
        ;res (set-schema :groups-schema-with-pagination (create-schema-by-data "groups" groups-meta-raw []))
        ;
        ;;; :groups-schema-response
        ;;update-schema-list-raw [{:column_name "id", :data_type "uuid" :is_nullable "NO" :required true}]
        ;update-schema-list-raw [{:column_name "id", :data_type "uuid"}]
        ;res (set-schema :groups-schema-response (create-schema-by-data "groups" groups-meta-raw [] [] update-schema-list-raw []))
        ;
        ;;; :groups-schema-response-put
        ;whitelist-key-names ["name" "type" "institution" "institutional_id" "institutional_name" "created_by_user_id"]
        ;res (set-schema :groups-schema-response-put (create-schema-by-data "groups" groups-meta-raw [] [] [] whitelist-key-names))
        ;
        ;
        ;;; :groups-schema-response-put-users
        ;;; example how to extract & merge meta-data-infos (PUT "/:group-id/users/")
        ;groups-users-meta-raw (concat (keep-maps-by-entry-values users-meta-raw ["email" "person_id"])
        ;                        (keep-maps-by-entry-values groups-meta-raw ["id" "institutional_id"]))
        ;
        ;res (set-schema :groups-schema-response-user-simple (create-schema-by-data "groups" groups-users-meta-raw))
        ;
        ;;; TODO: needed renaming of keys, fix handler to get rid of this workaround
        ;groups-users-meta-raw (update-column-value groups-users-meta-raw "person_id" "person-id")
        ;groups-users-meta-raw (update-column-value groups-users-meta-raw "institutional_id" "institutional-id")
        ;
        ;res (set-schema :groups-schema-response-put-users (create-schema-by-data "groups" groups-users-meta-raw)) ;; TODO: name of keys
        ]))


(defn create-users-schema []
  (let [

        ;; :groups-schema-raw
        ;; :groups-schema-response-put
        data {
              :raw [{:users {}}
                    {:_additional [{:column_name "is_admin", :data_type "boolean"}]}
                    ],
              :raw-schema-name :users-schema-raw
              :schemas [
                        {:users-schema-payload {:alias "maru.update/schema"
                                                :key-types "optional"
                                                :types [{:accepted_usage_terms_id {:value-type TYPE_MAYBE}} {:notes {:value-type TYPE_MAYBE}}]
                                                ;:wl ["accepted_usage_terms_id" "autocomplete" "email" "institution" "first_name" "last_name" "login" "note" "searchable"]
                                                :wl [:accepted_usage_terms_id :autocomplete :email :institution :first_name :last_name :login :note :searchable]
                                                }
                         }

                        {:get.users-schema-payload {:alias "mar.users.get/schema"
                                                    :value-types "maybe"
                                                    :types [{:created_at {:value-type TYPE_NOTHING}}
                                                            {:email {:key-type TYPE_OPTIONAL :value-type TYPE_NOTHING}}
                                                            {:id {:value-type TYPE_NOTHING}}
                                                            {:person_id {:value-type TYPE_NOTHING}}
                                                            {:is_admin {:value-type TYPE_NOTHING}}
                                                            {:updated_at {:value-type TYPE_NOTHING}}
                                                            {:settings {:key-type TYPE_OPTIONAL :value-type TYPE_NOTHING}}
                                                            ]
                                                    :bl [:searchable :active_until :autocomplete]
                                                    }
                         }

                        {:create.users-schema-payload {:alias "mar.users.create/schema"
                                                       :key-types "optional"

                                                       :types [
                                                               {:person_id {:key-type TYPE_NOTHING}}
                                                               {:accepted_usage_terms_id {:value-type TYPE_MAYBE}}
                                                               {:notes {:value-type TYPE_MAYBE}}
                                                               ]

                                                       :wl [:person_id :accepted_usage_terms_id :email :institution :institution_id :first_name :last_name :login :note :settings]
                                                       }
                         }
                        ]
              }
        res (create-raw-schema data)
        res2 (create-schemas-by-config data)
        p (println ">o> 3create-raw-schema (" (:raw-schema-name data) ") = " res)
        p (println ">o> 4create-raw-schema (" (:raw-schema-name data) ") = " res2)







        ;
        ;;; :users-schema-raw
        ;users-meta-raw (fetch-table-meta-raw "users")
        ;_ (set-schema :users-schema-raw users-meta-raw)
        ;
        ;;; :groups-schema-response-put
        ;whitelist-key-names ["id" "institutional_id" "email"]
        ;_ (set-schema :users-schema-payload (create-schema-by-data "users" users-meta-raw [] [] [] whitelist-key-names))
        ]))

(defn create-admins-schema []
  (let [
        ;; :users-schema-raw
        admins-meta-raw (fetch-table-meta-raw "admins")
        _ (set-schema :admins-schema-raw admins-meta-raw)

        _ (set-schema :admins-schema (create-schema-by-data "admins" admins-meta-raw))
        ]))


(defn create-workflows-schema []
  (let [
        ;; :workflows-schema-raw
        workflows-meta-raw (fetch-table-meta-raw "workflows")
        p (println ">o> workflows-meta-raw=" workflows-meta-raw)
        _ (set-schema :workflows-schema-raw workflows-meta-raw)


        _ (set-schema :workflows-schema (create-schema-by-data "workflows" workflows-meta-raw))

        whitelist-key-names ["name" "is_active" "configuration"]
        _ (set-schema :workflows-schema-min (create-schema-by-data "workflows" workflows-meta-raw [] [] [] whitelist-key-names))
        ]))


(def schema_sorting_types
  (s/enum "created_at ASC"
    "created_at DESC"
    "title ASC"
    "title DESC"
    "last_change"
    "manual ASC"
    "manual DESC"))

(defn create-collections-schema []
  (let [
        additional-order [
                          {:column_name "order", :data_type "enum::collections_sorting"}
                          {:column_name "me_get_metadata_and_previews", :data_type "boolean"}
                          {:column_name "me_edit_permission", :data_type "boolean"}
                          {:column_name "me_edit_metadata_and_relations", :data_type "boolean"}
                          ]
        additional-schema-list-raw (concat schema_pagination_raw schema_full_data_raw additional-order)

        ;; :workflows-schema-raw
        collections-meta-raw (fetch-table-meta-raw "collections" additional-schema-list-raw)
        p (println ">o> workflows-meta-raw=" collections-meta-raw)
        _ (set-schema :collections-schema-raw collections-meta-raw)
        _ (set-schema :collections-schema (create-schema-by-data "collections" collections-meta-raw))


        ;; :collections-schema-get
        whitelist-key-names ["collection_id" "creator_id" "responsible_user_id" "clipboard_user_id" "workflow_id" "responsible_delegation_id"
                             "public_get_metadata_and_previews"]

        ;additional-schema-list-raw (concat schema_pagination_raw schema_full_data_raw additional-order)

        collections-meta (update-column-value collections-meta-raw "id" "collection_id")
        collections-meta (update-column-value collections-meta "get_metadata_and_previews" "public_get_metadata_and_previews")

        ;_ (set-schema :collections-schema-get (create-schema-by-data "collections" collections-meta additional-schema-list-raw [] [] whitelist-key-names))
        _ (set-schema :collections-schema-get (create-schema-by-data "collections" collections-meta [] [] [] whitelist-key-names))



        ;; :collections-schema-put
        whitelist-key-names ["layout" "is_master" "sorting" "default_context_id" "workflow_id" "default_resource_type"
                             ]

        _ (set-schema :collections-schema-put (create-schema-by-data "collections" collections-meta-raw [] [] [] whitelist-key-names))


        p (println ">o> ???????? :collections-schema-get=" (get-schema :collections-schema-put))




        ;; :collections-schema-post
        whitelist-key-names ["layout" "is_master" "sorting" "default_context_id" "workflow_id" "default_resource_type"
                             "responsible_user_id" "responsible_delegation_id" "get_metadata_and_previews"
                             ]

        _ (set-schema :collections-schema-post (create-schema-by-data "collections" collections-meta-raw [] [] [] whitelist-key-names))

        p (println ">o> ???????? :collections-schema-get=" (get-schema :collections-schema-post))

        ]))



(defn create-collection-media-entry-schema []
  (let [
        db-table "collection_media_entry_arcs"

        ;; :workflows-schema-raw
        collections-meta-raw (fetch-table-meta-raw db-table)
        p (println ">o> collection_media_entry_arcs=" collections-meta-raw)
        _ (set-schema :collections-schema-media-entry-arcs-raw collections-meta-raw)
        _ (set-schema :collections-schema-media-entry-arcs (create-schema-by-data db-table collections-meta-raw))

        _ (set-schema :collections-schema-media-entry-arcs-get (create-schema-by-data db-table collections-meta-raw [] [] [] []))
        _ (set-schema :collections-schema-media-entry-arcs-modify (create-schema-by-data db-table collections-meta-raw [] [] [] ["highlight" "cover" "order" "position"]))
        ]))


(defn create-collection-collection-arcs-schema []
  (let [
        db-table "collection_collection_arcs"

        ;; :workflows-schema-raw
        collections-meta-raw (fetch-table-meta-raw db-table)
        p (println ">o> collection_collection_arcs=" collections-meta-raw)
        _ (set-schema :collections-schema-collection-arcs-raw collections-meta-raw)
        _ (set-schema :collections-schema-collection-arcs (create-schema-by-data db-table collections-meta-raw))

        _ (set-schema :collections-schema-collection-arcs-all (create-schema-by-data db-table collections-meta-raw [] [] [] []))
        _ (set-schema :collections-schema-collection-arcs-min (create-schema-by-data db-table collections-meta-raw [] [] [] ["highlight" "order" "position"]))
        ]))

(defn create-app-settings-schema []
  (let [
        db-table "app_settings"

        ;; :workflows-schema-raw
        collections-meta-raw (fetch-table-meta-raw db-table)
        p (println ">o> collection_collection_arcs=" collections-meta-raw)
        _ (set-schema :app-settings-schema-raw collections-meta-raw)
        _ (set-schema :app-settings-schema (create-schema-by-data db-table collections-meta-raw))

        _ (set-schema :app-settings-schema-all (create-schema-by-data db-table collections-meta-raw [] [] [] []))
        _ (set-schema :app-settings-schema-min (create-schema-by-data db-table collections-meta-raw [] ["created_at" "id" "updated_at" "users_active_until_ui_default"] [] []))

        p (println ">o> >>> create-app-settings-schema >>> " (get-schema :app-settings-schema-min))
        ]))

(defn create-confidential-links-schema []
  (let [
        db-table "confidential_links"

        ;; :workflows-schema-raw
        confidential-links-raw (fetch-table-meta-raw db-table)
        p (println ">o> confidential_links=" confidential-links-raw)
        _ (set-schema :confidential-links-schema-raw confidential-links-raw)
        _ (set-schema :confidential-links-schema (create-schema-by-data db-table confidential-links-raw))

        _ (set-schema :confidential-links-schema-all (create-schema-by-data db-table confidential-links-raw [] [] [] []))
        _ (set-schema :confidential-links-schema-min (create-schema-by-data db-table confidential-links-raw [] [] [] ["revoked" "description" "expires_at"]))

        p (println ">o> >>> create-app-settings-schema >>> " (get-schema :app-settings-schema-min))
        ]))

(defn create-context-keys-schema []
  (let [
        db-table "context_keys"

        ;; :workflows-schema-raw
        context-keys-raw (fetch-table-meta-raw db-table)
        p (println ">o> confidential_links=" context-keys-raw)
        _ (set-schema :context-keys-raw context-keys-raw)
        _ (set-schema :context-keys (create-schema-by-data db-table context-keys-raw))
        _ (set-schema :context-keys-all (create-schema-by-data db-table context-keys-raw))

        _ (set-schema :context-keys-min (create-schema-by-data-bl db-table context-keys-raw ["admin_comment" "updated_at" "created_at"]))
        _ (set-schema :context-keys-update (create-schema-by-data-bl db-table context-keys-raw ["id" "context_id" "meta_key_id" "updated_at" "created_at"]))
        _ (set-schema :context-keys-import (create-schema-by-data-bl db-table context-keys-raw ["id" "updated_at" "created_at"]))

        p (println ">o> >>> context-keys >>> " (get-schema :context-keys-import))
        ]))


(defn create-context-schema []
  (let [
        db-table "contexts"

        ;; :workflows-schema-raw
        context-keys-raw (fetch-table-meta-raw db-table)
        p (println ">o> confidential_links=" context-keys-raw)
        _ (set-schema :contexts-raw context-keys-raw)
        _ (set-schema :contexts (create-schema-by-data db-table context-keys-raw))
        _ (set-schema :contexts-all (create-schema-by-data db-table context-keys-raw))

        _ (set-schema :contexts-update (create-schema-by-data-bl db-table context-keys-raw ["id"]))
        _ (set-schema :contexts-export (create-schema-by-data-bl db-table context-keys-raw ["admin_comment"]))

        p (println ">o> >>> contexts >>> " (get-schema :contexts-export))
        ]))

(defn create-custom-urls-schema []
  (let [
        db-table "custom_urls"

        ;; :workflows-schema-raw
        context-keys-raw (fetch-table-meta-raw db-table)
        p (println ">o> confidential_links=" context-keys-raw)
        _ (set-schema :custom-urls-raw context-keys-raw)
        _ (set-schema :custom-urls-all (create-schema-by-data db-table context-keys-raw))
        _ (set-schema :custom-urls-min (create-schema-by-data-wl db-table context-keys-raw ["id" "is_primary"]))

        p (println ">o> >>> contexts >>> " (get-schema :custom-urls-all))
        ]))


(defn create-delegation-schema []
  (let [
        db-table "delegations"

        ;; :workflows-schema-raw
        context-keys-raw (fetch-table-meta-raw db-table)
        p (println ">o> >>> delegations.raw >>> " context-keys-raw)
        _ (set-schema :delegations-raw context-keys-raw)
        _ (set-schema :delegations-all (create-schema-by-data db-table context-keys-raw))
        _ (set-schema :delegations-min (create-schema-by-data-bl db-table context-keys-raw ["id"]))

        p (println "\n\n>o> >>> delegations.all >>> " (get-schema :delegations-all))
        p (println ">o> >>> delegations.min >>> " (get-schema :delegations-min))

        ;_ (System/exit 0)

        ]))

(defn rename-by-keys
  [maps key-map]

  (println ">o> >>??>> rename-by-keys=" maps key-map)

  (map
    (fn [m]
      (reduce
        (fn [acc [old-key new-key]]
          (if (contains? m old-key)
            (assoc acc new-key (m old-key))
            acc))
        (apply dissoc m (keys key-map))
        key-map))
    maps))













;(defn create-test-schema []
;  (let [
;
;        ;; featues
;        ;; - rename raw-schema
;        ;; - blacklist & whitelist
;        ;; - add additinal schema
;
;        data {
;
;              ;{:column_name "count", :data_type "int4" :key-type TYPE_OPTIONAL}])
;
;
;              :raw [
;                    {:groups {:wl ["name" "id"] :rename {"name" "my-name"}}}
;                    ;{:groups {:wl [:name :id]}}
;                    {:_additional [{:column_name "my-int" :data_type "int4"} {:column_name "date-id" :data_type "uuid"} {:column_name "my-enum" :data_type "enum::collections_sorting"}]}
;                    {:delegations {:bl ["id" "name"] :rename {"admin_comment" "my-comment"}}}
;                    ;{:delegations {:bl ["id" "name"]} }
;                    ],
;              :raw-schema-name :schema-groups-raw,
;
;              :schemas [
;                        ;{:put.body {:bl ["id" "dates" "my-int" "my-enum"]}}
;
;                        ;; works?
;                        ;{:put.body1 {:bl [:id :dates :my-int :my-enum]}}
;                        ;{:put.body2 {:wl [:id :dates :my-int :my-enum]}}
;                        ;{:put.body3 {}}
;
;
;
;                        ;; TODO: support :types
;
;                        {:get.body {
;                                    ;:wl ["id" "name"],
;                                    ;:wl [:id :date-id :name :my-int :my-enum]
;                                    :wl [:id :date-id :my-int :my-enum]
;                                    :types [
;                                            {:my-int {:key-type "optional" :value-type "maybe"}}
;                                            ;{:id {:value-type nil}}
;                                            {:id {:key-type "required" :value-type nil}}
;                                            ]
;                                    }}
;
;                        {:post.body {
;                                     :bl [:id :sum],
;                                     :types [
;                                             {:my-comment {:key-type "optional" :value-type "maybe"}}
;                                             {:id {:value-type nil}}
;                                             ]
;                                     }}
;                        ]
;
;              }
;
;
;        p (println ">o> >>>>> BEFORE >>>>>>")
;        res (create-raw-schema data)
;
;
;
;        p (println ">o> >>>> AFTER >>>>>>> res=" res)
;
;        _ (println ">o> LOADED / final res=" (get-schema :get.body))
;
;
;        res (create-schemas-by-config data)
;
;        ]))





(defn init-schema-by-db []

  (println ">o> before db-fetch")
  ;(fetch-table-metadata "groups")
  ;(prepare-schema "groups")

  (let [

        ;;; init enums
        _ (set-enum :collections_sorting (create-enum-spec "collection_sorting"))
        _ (set-enum :collections_layout (create-enum-spec "collection_layout"))
        _ (set-enum :collections_default_resource_type (create-enum-spec "collection_default_resource_type"))
        ;
        ;
        ;
        ;
        ;te_pr (println ">o> 11??=" :collections_sorting (get-enum :collections_sorting))
        ;te_pr (println ">o> 11??=" :collections_layout (get-enum :collections_layout))
        ;te_pr (println ">o> 11??=" :collections_default_resource_type (get-enum :collections_default_resource_type))
        ;
        ;;;; TODO: revise db-ddl to use enum
        _ (set-enum :groups.type (s/enum "AuthenticationGroup" "InstitutionalGroup" "Group"))
        ;
        _ (create-groups-schema)
        _ (create-users-schema)

        _ (create-admins-schema)
        ;_ (create-workflows-schema)
        ;_ (create-collections-schema)
        ;_ (create-collection-media-entry-schema)
        ;_ (create-collection-collection-arcs-schema)
        ;_ (create-app-settings-schema)
        ;_ (create-confidential-links-schema)
        ;_ (create-context-keys-schema)
        ;_ (create-context-schema)
        ;_ (create-custom-urls-schema)
        ;_ (create-delegation-schema)

        ;_ (create-test-schema)

        ]))




;(ns leihs.my.back.html
;    (:refer-clojure :exclude [keyword str])
;    (:require
;      [hiccup.page :refer [html5]]
;      [honey.sql :refer [format] :rename {format sql-format}]
;      [honey.sql.helpers :as sql]
;      [leihs.core.http-cache-buster2 :as cache-buster]
;      [leihs.core.json :refer [to-json]]
;      [leihs.core.remote-navbar.shared :refer [navbar-props]]
;      [leihs.core.shared :refer [head]]
;      [leihs.core.url.core :as url]
;      [leihs.my.authorization :as auth]
;      [leihs.core.db :as db]
;      [next.jdbc :as jdbc]))

(comment
  (let [
        data {
              :raw [
                    {:groups {}}
                    ;{:_additional (concat schema_pagination_raw schema_full_data_raw)}
                    ]
              :raw-schema-name :groups-schema-with-pagination-raw
              :schemas [
                        {:groups.schema-query-groups {:key-types "optional" :alias "schema_query-groups"}}
                        ]
              }

        tx (get-ds)

        ;data {:foo "bar" :baz "qux"}

        data [:cast (json/generate-string data) :jsonb]


        ins-data {:id "abc" :key "test-me" :config data}

        query (-> (sql/insert-into :schema_definition)
                  (sql/values [ins-data])
                  (sql/returning :*)
                  sql-format
                  )

        ;query (-> (sql/select :*)
        ;          (sql/from :schema_definition)
        ;          sql-format
        ;          )

        p (println ">o> query=" query)

        res (jdbc/execute! tx query)


        p (println "\nquery" res)

        ]
    res
    )
  )


(defn- context_transform_ml [context]
  (assoc context
         :config (sd/transform_ml (:config context))
         ;:descriptions (sd/transform_ml (:descriptions context))
         ))

(comment
  (let [
        tx (get-ds)

        query (-> (sql/select :*)
                  (sql/from :schema_definition)
                  (sql/where [:= :key "test-me"])
                  sql-format
                  )

        p (println ">o> query=" query)

        res (jdbc/execute-one! tx query)
        p (println ">o> res1=" res)
        ]
    res
    )
  )

