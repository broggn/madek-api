(ns madek.api.db.dynamic_schema.core
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.db.dynamic_schema.common :refer [get-enum get-schema set-enum set-schema]]
   [madek.api.db.dynamic_schema.schema_definitions :refer [type-mapping type-mapping-enums]]
   [madek.api.db.dynamic_schema.statics :refer [TYPE_EITHER TYPE_MAYBE TYPE_NOTHING TYPE_OPTIONAL TYPE_REQUIRED]]
   [next.jdbc :as jdbc]
   [schema.core :as s]
   [taoensso.timbre :refer [spy]])
  )

(require '[schema.core :as schema])

(defn fetch-enum [enum-name]
  (println ">o> fetch-enum by DB!!!!!!!!")
  (let [ds (get-ds)]
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
  (apply s/enum (mapv #(:enumlabel %) enum-data)))

(defn create-enum-spec [table-name]
  (let [enum (fetch-enum table-name)]
    (convert-to-enum-spec enum)))


(defn init-enums-by-db []
  (let [
        ;;; init enums
        _ (set-enum :collections_sorting (create-enum-spec "collection_sorting"))
        _ (set-enum :collections_layout (create-enum-spec "collection_layout"))
        _ (set-enum :collections_default_resource_type (create-enum-spec "collection_default_resource_type"))

        ;;;; TODO: revise db-ddl to use enum
        _ (set-enum :groups.type (s/enum "AuthenticationGroup" "InstitutionalGroup" "Group"))
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
  (let [mkey (keyword (str table-name "." (row :column_name)))
        new-data-type (get enum-map mkey)]
    (if new-data-type
      (assoc row :data_type new-data-type)
      row)))

(defn remove-maps-by-entry-values
  "Removes maps from a list where the specified entry key matches any of the values in the provided list."

  ([maps target-values]
   (remove-maps-by-entry-values maps :column_name target-values)
   )

  ;; TODO: fix this to handle [:foo :bar]
  ([maps entry-key target-values]
   (if (empty? target-values)
     maps
     (remove #(some #{(entry-key %)} target-values) maps))))



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
     (filter #(some #{(entry-key %)} target-values) maps))))


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
   (let [res (fetch-table-metadata table-name)
         res (concat res update-data)]
     res)))

(defn postgres-cfg-to-schema [table-name metadata]
  (into {}
    (map (fn [{:keys [column_name data_type key-type value-type]}]
           (println ">o> postgres-cfg-to-schema =>" table-name column_name data_type key-type value-type)

           (let [keySection (cond (= key-type TYPE_REQUIRED) (s/required-key (keyword column_name))
                                  (= key-type TYPE_OPTIONAL) (s/optional-key (keyword column_name))
                                  :else (keyword column_name))

                 ;; FYI, expected: <table>.<column> eg.: "groups.type"
                 type-mapping-key (str (name table-name) "." column_name)
                 type-mapping-enums-res (type-mapping-enums type-mapping-key get-enum)
                 type-mapping-res (type-mapping data_type)

                 valueSection (cond
                                ;(str/starts-with? data_type "enum::") (get-enum (keyword (str/replace data_type #"enum::" "")))
                                (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                                      (s/maybe type-mapping-enums-res)
                                                                      type-mapping-enums-res)
                                (not (nil? type-mapping-res)) (if (= value-type TYPE_MAYBE)
                                                                (s/maybe type-mapping-res)
                                                                type-mapping-res)
                                (= value-type TYPE_MAYBE) (s/maybe s/Any)
                                :else s/Any)


                 p (println ">o> [postgres-cfg-to-schema] table= " table-name ", final-result =>> " {keySection valueSection})
                 p (println ">oo>  --------------------------------")
                 ]
             {keySection valueSection}))
      metadata)))

(defn rename-column-names
  [maps key-map]
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
                          (cond
                            (not (str/starts-with? (name key) "_"))
                            (let [p (println ">o> [handle not-additional]")
                                  table-name key
                                  wl-attr (:wl value)
                                  bl-attr (:bl value)
                                  rename-attr (:rename value)
                                  key (name key)
                                  res (fetch-table-metadata key)

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
                                  res2 (cond
                                         (not (nil? bl-attr)) (remove-maps-by-entry-values res :column_name bl-attr)
                                         (not (nil? wl-attr)) (keep-maps-by-entry-values res :column_name wl-attr)
                                         :else res)
                                  res3 (postgres-cfg-to-schema table-name res2)]
                              (into inner-acc res3))        ; Concat res2 into inner-acc

                            (= (name key) "_additional")
                            (let [p (println ">o> [handle _additional]")

                                  table-name key

                                  res2 value

                                  res2 (postgres-cfg-to-schema table-name res2)]

                              (into inner-acc res2))
                            :else inner-acc))
                  acc item))
              result raw)

        _ (set-schema raw-schema-name res)]
    res))

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
  (let [res (fetch-enum table-name)
        res (convert-to-enum-spec res)
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

        val (fetch-value-by-key types column_name)
        key-type (get val :key-type)
        value-type (get val :value-type)
        either-condition (get val :either-condition)
        key-type (if (not (nil? key-types))
                   (if (nil? key-type) key-types key-type)
                   key-type)
        value-type (if (not (nil? value-types))
                     (if (nil? value-type) value-types value-type)
                     value-type)

        keySection (cond (= key-type TYPE_REQUIRED) (s/required-key (keyword column_name))
                         (= key-type TYPE_OPTIONAL) (s/optional-key (keyword column_name))
                         (= key-type TYPE_NOTHING) (keyword column_name)
                         :else (keyword column_name))


        ;; revise schema types by mapping
        type-mapping-key (str (name table-name) "." (name column_name))
        type-mapping-enums-res (type-mapping-enums type-mapping-key get-enum)
        valueSection (cond
                       (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                             (s/maybe type-mapping-enums-res)
                                                             type-mapping-enums-res)
                       (and (= value-type TYPE_EITHER) (not (nil? either-condition))) (s/->Either either-condition)
                       (= value-type TYPE_MAYBE) (s/maybe column_type)
                       :else column_type)


        p (println ">o> [revise-schema-types] <<<<<<<<<<<<<<< before <<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n
        type-mapping-key=" type-mapping-key "\n
        column_name=" column_name "\n
        key-type=" key-type "\n
        value-type=" value-type "\n
        ")

        ;; TODO: quiet helpful for debugging
        p (println ">>o> !!! [set-schema] =>> " {keySection valueSection})
        ]
    {keySection valueSection}))

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
  (remove (fn [[key _]]
            (some #{key} keys-to-remove)) data))

(defn keep-by-keys [data keys-to-keep]
  (filter (fn [[key _]]
            (some #{key} keys-to-keep)) data))

(defn create-schemas-by-config [data]
  (let [
        schema-def (:schemas data)
        raw-schema-name (:raw-schema-name data)

        _ (if (nil? schema-def)
            (throw (Exception. "[create-schemas-by-config()] No data.schemas definition found")))

        _ (if (nil? raw-schema-name)
            (throw (Exception. "[create-schemas-by-config()] No data.raw-schema-name definition found")))

        result []
        res (reduce
              (fn [acc item]
                (reduce (fn [inner-acc [key value]]
                          (let [schema-raw (get-schema raw-schema-name)
                                table-name key
                                wl-attr (:wl value)
                                bl-attr (:bl value)
                                cache-as (:cache-as value)

                                types-attr (:types value)
                                key-types (:key-types value)
                                value-types (:value-types value)

                                ;key (name key)
                                res schema-raw
                                res2 (cond
                                       (not (nil? bl-attr)) (remove-by-keys res bl-attr)
                                       (not (nil? wl-attr)) (keep-by-keys res wl-attr)
                                       :else res)

                                res2 (process-revision-of-schema-types table-name res2 types-attr key-types value-types)

                                _ (set-schema (keyword key) res2)
                                _ (when (not (nil? cache-as))
                                    (doseq [kw cache-as]
                                      (set-schema (keyword kw) res2)))]

                            (into inner-acc res2)))
                  acc item))
              result schema-def)
        ] res))


(defn create-dynamic-schema [cfg-array]
  (doseq [c cfg-array]
    (let [_ (create-raw-schema c)
          _ (when (contains? c :schemas)
              (create-schemas-by-config c)
              )])))

