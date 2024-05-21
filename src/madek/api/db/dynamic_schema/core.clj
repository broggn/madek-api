(ns madek.api.db.dynamic_schema.core
  (:require
   ;; all needed imports
   ;[leihs.core.db :as db]

   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]

   [honey.sql.helpers :as sql]


   [madek.api.db.core :refer [get-ds]]

   ;[madek.api.db.dynamic_schema.schema_cache :refer [get-enum set-enum]]

   [madek.api.db.dynamic_schema.schema_definitions :refer [raw-type-mapping type-mapping type-mapping-enums]]
   [madek.api.db.dynamic_schema.statics :refer [TYPE_EITHER TYPE_MAYBE TYPE_NOTHING TYPE_OPTIONAL TYPE_REQUIRED]]

   ;[madek.api.utils.helper :refer [merge-query-parts to-uuids]]

   ; [taoensso.timbre :as timbre]
   ; [clojure.core.cache :as cache]
   [next.jdbc :as jdbc]

   [schema.core :as s]
   [schema.core :as s]
   [taoensso.timbre :refer [spy]]


   ; [taoensso.timbre :as timbre]
   ; [clojure.core.cache :as cache]
   [taoensso.timbre :refer [spy]])
  )

(defn pr [key fnc]
  (println ">oo> HELPER / " key "=" fnc)
  fnc
  )



(def schema-cache (atom {}))





;(def TYPE_NOTHING "nothing")
;(def TYPE_REQUIRED "required")
;(def TYPE_OPTIONAL "optional")
;(def TYPE_MAYBE "maybe")
;(def TYPE_EITHER "either")


(require '[schema.core :as schema])

;;;; db-operations

;;;; cache access helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def enum-cache (atom {}))
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

        ] res))


(defn set-enum [key value]
  (println ">oo> set-enum.key=" key)
  (swap! enum-cache assoc key value))


(defn init-enums-by-db []

  (let [

        ;;; init enums
        _ (set-enum :collections_sorting (create-enum-spec "collection_sorting"))
        _ (set-enum :collections_layout (create-enum-spec "collection_layout"))
        _ (set-enum :collections_default_resource_type (create-enum-spec "collection_default_resource_type"))

        ;;;; TODO: revise db-ddl to use enum
        _ (set-enum :groups.type (s/enum "AuthenticationGroup" "InstitutionalGroup" "Group"))
        ;
        ])

  )




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





;; TODO: remove this
(defn process-raw-type-mapping [table-name metadata]

  (let [

        ;raw-type-mapping {
        ;                  ;:delegations.test "enum::test"
        ;                  ;:groups.id "enum::what-the-fuck"
        ;                  :groups.type "enum::groups.type"
        ;
        ;                  }

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
         res (process-raw-type-mapping table-name res)

         ;res (map normalize-map res)
         ;p (println ">o> 2res=" res)


         ;res (replace-elem res update-data :column_name)    ;;??

         ] res))
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

                 type-mapping-enums-res (type-mapping-enums type-mapping-key get-enum)
                 type-mapping-res (type-mapping data_type)  ;raw-mapping

                 p (println ">>o> !!! [set-schema] =>> key=" type-mapping-key ", type=" data_type ", type: >" type-mapping-res "<")


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



(defn set-schema [key value]
  (let [
        ;; TODO: quiet helpful for debugging
        p (println ">o> !!! [set-schema] (" key ") ->" value)

        value (into {} value)

        res (swap! schema-cache assoc key value)
        ] res)
  )



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








(defn rename-by-keys
  [maps key-map]
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



(defn create-enum-spec [table-name]
  (let [
        res (fetch-enum table-name)
        p (println ">o> 1ares=" res)
        res (convert-to-enum-spec res)
        p (println ">o> 2ares=" res)

        ] res))



(defn fetch-value-by-key
  [maps key]
  (some (fn [m] (get m key)) maps))


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
        either-condition (get val :either-condition)
        ;key-type (:key-type val)
        ;value-type (:value-type val)


        ;key-type (get-in types [(keyword column_name) :key-type])
        ;value-type (get-in types [(keyword column_name) :value-type])




        ;p (println ">o> val=" val)
        p (println ">o> >>>>>>>> val=" val)
        p (println ">o> >>>>>>>> key-type=" key-type)
        p (println ">o> >>>>>>>> value-type=" value-type)

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
                         ;(= key-type TYPE_EITHER) (s/optional-key (keyword column_name))
                         (= key-type TYPE_NOTHING) (keyword column_name)
                         :else (do
                                 ;(println ">o> nix")
                                 (keyword column_name))
                         )



        _ (when (= key-type TYPE_EITHER) (throw (Exception. "TYPE_EITHER not supported")))

        ;; revise schema types by mapping
        type-mapping-key (str (name table-name) "." (name column_name))
        type-mapping-enums-res (type-mapping-enums type-mapping-key get-enum)
        ;p (println ">o> ?????????? type-mapping-key => " type-mapping-key "/" type-mapping-enums-res)
        ;
        ;p (println ">o> ?????????? value-type=" value-type)
        valueSection (cond
                       (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                             (s/maybe type-mapping-enums-res)
                                                             type-mapping-enums-res)
                       (and (= value-type TYPE_EITHER) (not (nil? either-condition))) (s/->Either either-condition)
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

        ;; TODO: quiet helpful for debugging
        p (println ">>o> !!! [set-schema] =>> " {keySection valueSection})


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






;;;;; cache access helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(defn get-enum [key & [default]]
;
;  (let [
;        val (get @enum-cache key default)
;        p (println ">o> key=" key)
;        p (println ">o> val=" val)
;        p (println ">o> default=" default)
;
;        ] val)
;
;  ;(println ">oo> get-enum.key=" key)
;  ;(pr key (get @enum-cache key default))
;  )
;
;
;(defn set-enum [key value]
;  (println ">oo> set-enum.key=" key)
;  (swap! enum-cache assoc key value))


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
                                cache-as (:cache-as value)

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
                                ;p (println ">o> !!! [set-schema] ?" (keyword key) "/" res2)

                                _ (set-schema (keyword key) res2)
                                _ (when (not (nil? cache-as))
                                    (doseq [kw cache-as]
                                      (set-schema (keyword kw) res2)))


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




(defn create-dynamic-schema [cfg-array]
  (doseq [c cfg-array]
    (let [_ (create-raw-schema c)
          _ (when (contains? c :schemas)
              (create-schemas-by-config c)
              )])))













