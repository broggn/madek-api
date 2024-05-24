(ns madek.api.db.dynamic_schema.core
  (:require
   [clojure.string :as str]
   [madek.api.db.dynamic_schema.common :refer [add-to-validation-cache get-enum get-schema set-enum set-schema]]
   [madek.api.db.dynamic_schema.db :refer [fetch-enum fetch-table-metadata]]
   [madek.api.db.dynamic_schema.schema_definitions :refer [type-mapping type-mapping-enums]]
   [madek.api.db.dynamic_schema.schema_logger :refer [slog]]
   [madek.api.db.dynamic_schema.statics :refer [TYPE_EITHER TYPE_MAYBE TYPE_NOTHING TYPE_OPTIONAL TYPE_REQUIRED]]
   [schema.core :as s]))

(require '[schema.core :as schema])

(defn convert-to-enum-spec
  "Creates a Spec enum definition from a sequence of maps with namespaced keys."
  [enum-data]
  (apply s/enum (mapv #(:enumlabel %) enum-data)))

(defn create-enum-spec [table-name]
  (let [enum (fetch-enum table-name)]
    (convert-to-enum-spec enum)))

(defn init-enums-by-db []
  (let [;;; init enums
        _ (set-enum :collections_sorting (create-enum-spec "collection_sorting"))
        _ (set-enum :collections_layout (create-enum-spec "collection_layout"))
        _ (set-enum :collections_default_resource_type (create-enum-spec "collection_default_resource_type"))

        ;;;; TODO: revise db-ddl to use enum
        _ (set-enum :groups.type (s/enum "AuthenticationGroup" "InstitutionalGroup" "Group"))]))

(defn remove-maps-by-entry-values
  "Removes maps from a list where the specified entry key matches any of the values in the provided list."

  ([maps target-values]
   (remove-maps-by-entry-values maps :column_name target-values))

  ;; TODO: fix this to handle [:foo :bar]
  ([maps entry-key target-values]
   (if (empty? target-values)
     maps
     (remove #(some #{(entry-key %)} target-values) maps))))

(defn keep-maps-by-entry-values
  "Keeps only maps from a list where the specified entry key matches any of the values in the provided list."
  ([maps target-values]
   (keep-maps-by-entry-values maps :column_name target-values))

  ;; TODO: fix this to handle [:foo :bar]
  ([maps entry-key target-values]
   (if (empty? target-values)
     maps
     (filter #(some #{(entry-key %)} target-values) maps))))

(defn fetch-table-meta-raw
  ([table-name]
   (fetch-table-meta-raw table-name []))

  ([table-name update-data]
   (let [res (fetch-table-metadata table-name)
         res (concat res update-data)]
     res)))

(defn postgres-cfg-to-schema [table-name metadata]
  (into {}
        (map (fn [{:keys [column_name data_type key-type value-type]}]
               (let [keySection (cond (= key-type TYPE_REQUIRED) (s/required-key (keyword column_name))
                                      (= key-type TYPE_OPTIONAL) (s/optional-key (keyword column_name))
                                      :else (keyword column_name))

                 ;; FYI, expected: <table>.<column> eg.: "groups.type"
                     type-mapping-key (str (name table-name) "." column_name)
                     type-mapping-enums-res (type-mapping-enums type-mapping-key get-enum)
                     type-mapping-res (type-mapping data_type)

                     valueSection (cond
                                    (not (nil? type-mapping-enums-res)) (if (= value-type TYPE_MAYBE)
                                                                          (s/maybe type-mapping-enums-res)
                                                                          type-mapping-enums-res)
                                    (not (nil? type-mapping-res)) (if (= value-type TYPE_MAYBE)
                                                                    (s/maybe type-mapping-res)
                                                                    type-mapping-res)
                                    (= value-type TYPE_MAYBE) (s/maybe s/Any)
                                    :else s/Any)

                     _ (slog (str "[postgres-cfg-to-schema] table= " table-name ", final-result =>> " {keySection valueSection}))]
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

;(defn keys-exist? [key-maps keys-array]
;
;  (println "\n>o> keys-not-exist?1: " key-maps)
;  (println ">o> keys-not-exist?1: " keys-array)
;
;  (every? #(some #{(:column_name %)} keys-array) key-maps))
;
;(defn keys-not-exist? [key-maps keys-array]
;
;  (println "\n>o> keys-not-exist?1: " key-maps)
;  (println ">o> keys-not-exist?1: " keys-array)
;
;  (some #(not (some #{(:column_name %)} keys-array)) key-maps))
;

;(defn keys-exist? [key-maps keys-array]
;  (println "\n>o> keys-exist?: " key-maps)
;  (println ">o> keys-array: " keys-array)
;
;  (let [missing-keys (filter #(not (some #{(:column_name %)} keys-array)) key-maps)]
;    (when (not (empty? missing-keys))
;      (println ">o> Missing keys: " (map :column_name missing-keys))
;      (System/exit 0)
;      )
;    (empty? missing-keys)))

(defn keys-exist?x [db-meta keys debug-info]
  ;(println "\n>o> keys-exist?: " db-meta)
  (println ">o> ????? keys=" keys)
  (println ">o> ????? db-meta=" db-meta)
  (println ">o> ????? db-meta=" (class db-meta))
  (println ">o> ????? db-meta=" (type db-meta))

  (let [;existing-keys (cond
        ;                (instance? PersistentVector db-meta)
        ;                (do
        ;                  (println ">o> ????? clojure.lang.PersistentVector")
        ;                  (set (map :column_name db-meta)))
        ;
        ;
        ;                (instance? PersistentHashMap db-meta)
        ;                (do
        ;                  (println ">o> ????? X clojure.lang.PersistentHashMap")
        ;                  (println ">o> ????? X" db-meta)
        ;                  ;(set (keys db-meta)))
        ;                  (reduce (fn [acc key] (conj acc key)) [] (keys db-meta)))
        ;
        ;
        ;                :else
        ;                (do
        ;                  (println ">o> ????? db-meta=" (class db-meta))
        ;                  (System/exit 0))
        ;                )
        ;

        existing-keys (set (map :column_name db-meta))
        p (println ">o> ????? existing-keys=" existing-keys)

        missing-keys (filter #(not (contains? existing-keys %)) keys)
        p (println ">o> ????? missing-keys=" missing-keys)
        p (println ">o> ????? --------------------------------")]

    (when (not (empty? missing-keys))
      (let [error-msg (str "ERROR: Incorrect definition of key(s): " (str/join ", " missing-keys))
            debug-info (update debug-info :error conj error-msg)
            _ (add-to-validation-cache debug-info)]))
    (empty? missing-keys)))

(defn keys-exist? [db-meta keys debug-info]
  ;(println "\n>o> keys-exist?: " db-meta)
  ;(println ">o> keys: " keys)
  (println ">o> XXX debug-info=" debug-info)
  (println ">o> XXX db-meta=" db-meta)
  ;(println ">o> ?????2 db-meta=" (class db-meta))
  ;(println ">o> ?????2b db-meta=" (type db-meta))

  (let [existing-keys (cond
                        (or (instance? clojure.lang.PersistentVector db-meta) (instance? clojure.lang.LazySeq db-meta)) ;; OK
                        (do
                          ;(println ">o> ?????3 clojure.lang.PersistentVector")
                          (set (map :column_name (into [] db-meta))))

                        (instance? clojure.lang.PersistentHashMap db-meta)

                        (let [;p (println ">o> abc--------------")
                              all-keys (into [] (map first db-meta))
                              ;_ (println ">o> 1sart" all-keys)
                              all-keys2 (into [] (map second db-meta))
                              ;_ (println ">o> 2sart" all-keys2)

                              db-meta (zipmap all-keys all-keys2)
                              ;_ (println ">o> 3sart" db-meta)

                              ;p (println ">o> ????? before-res=" all-keys)

                              ;res (into [] (map (comp name first) db-meta))
                              res (into #{} (map (comp name first) db-meta))

                              ;p (println ">o> ????? after-res=" res)

                              ;_ (println ">o> 1sart" res)
                              ;_ (println ">o> 1sart" (first res))
                              ;_ (println ">o> 1sart" (type (first res)))
                              ]
                          res)

                        (instance? clojure.lang.PersistentArrayMap db-meta)

                        (let [p (println ">o> XX abc------HERE--------")
                              all-keys (into [] (map first db-meta))
                              _ (println ">o> XX 1sart" all-keys)
                              all-keys2 (into [] (map second db-meta))
                              _ (println ">o> XX 2sart" all-keys2)

                              db-meta (zipmap all-keys all-keys2)
                              _ (println ">o> XX 3sart" db-meta)

                              ;p (println ">o> ????? before-res=" all-keys)

                              ;res (into [] (map (comp name first) db-meta))
                              res (into #{} (map (comp name first) db-meta))

                              p (println ">o> XX ????? after-res=" res)

                              _ (println ">o> XX 1sart" res)
                              _ (println ">o> XX 1sart" (first res))
                              _ (println ">o> XX 1sart" (type (first res)))]

                          res)

                        :else
                        (do
                          (println ">o> ERROR: Something failed, missing handler for db-meta.type=" (class db-meta))
                          ;(println ">o> type?" (type db-meta))
                          ;(println ">o> db-meta="  db-meta)
                          ;(println ">o> db-meta="  (set (map :column_name (into [] db-meta))))
                          (System/exit 0)))

        ;;existing-keys (set (map :column_name db-meta))
        ;p (println ">o> ?????6 existing-keys=" existing-keys)
        ;p (println ">o> ?????6 aa existing-keys=" (type (first existing-keys)))
        ;p (println ">o> ?????6 keys=" keys)
        ;p (println ">o> ?????6 keys=" (type (first keys)))

        p (println ">o> XX ????? existing-keys=" existing-keys)
        p (println ">o> XX ????? bef-keys=" keys)
        keys (map name keys)
        p (println ">o> XX ????? aft-keys=" keys)

        missing-keys (filter #(not (contains? existing-keys %)) keys)
        p (println ">o> XX ?????7 xx missing-keys=" missing-keys)
        p (println ">o> XX ?????8 --------------------------------")]

    (when (not (empty? missing-keys))
      (let [error-msg (str "ERROR: Incorrect definition of key(s): " (str/join ", " missing-keys))
            debug-info (update debug-info :error conj error-msg)
            _ (add-to-validation-cache debug-info)]))
    (empty? missing-keys)))

(defn convert-persistent-hashmap-to-map [phm]
  (reduce (fn [acc [k v]]
            (assoc acc k v))

          {}
    ;(hash-map)
    ;(clojure.lang.PersistentHashMap/EMPTY)
          phm))

(defn convert-keys-to-integers [m]
  (into {} (map (fn [[k v]] [(str k) v]) m)))

(defn convert-keys-to-integers [m]
  (into {} (map (fn [[k v]] [(hash k) v]) m)))

(comment
  (let [; works
        ;keys ["collection_id" "order" "creator_id" ]
        ;map2 [{:column_name "default_resource_type", :data_type "USER-DEFINED"} {:column_name "get_metadata_and_previews", :data_type "boolean"}]

        ;; works / OK
        ;keys (vector "collection_id" "order" "creator_id" )
        keys (vector "collection_id")
        keys (vector "institution")
        ;keys (vector "collection_id" "order"  )
        map2 (vector {:column_name "collection_id", :data_type "USER-DEFINED"} {:column_name "order", :data_type "boolean"})

        ;menum (s/enum "AuthenticationGroup" "InstitutionalGroup" "Group")
        ;map2 (hash-map :institution java.lang.String, :institutional_id java.lang.String, :name java.lang.String,
        ;       :type menum
        ;       :institutional_name java.lang.String, :created_at s/Any)

        map2 (clojure.lang.PersistentHashMap/create {:institution "Institution"
                                                     :institutional_id "ID"
                                                     :name "Name"
                                                     :type "Type"
                                                     :institutional_name "Institutional Name"
                                                     :created_at (java.util.Date.)})

        ;map2 (hash-map :institution "Institution"
        ;                                                   :institutional_id "ID"
        ;                                                   :name "Name"
        ;                                                   :type "Type"
        ;                                                   :institutional_name "Institutional Name"
        ;                                                   :created_at (java.util.Date.))

        ;all-keys (into [] (map first map2))
        ;all-keys2 (into [] (map second map2))
        ;
        ;map2 (zipmap all-keys all-keys2)
        ;;all-keys (keys map2)
        ;_ (println ">o> sart" all-keys)
        ;_ (println ">o> sart" all-keys2)
        ;_ (println ">o> sart" map2)
        ;_ (println ">o> sart" (keys map2))
        ]
    (keys-exist?2 map2 keys {:foo "bar"})))

(comment
  (let [; works
        keys ["collection_id" "order"]
        keys ["collection_id"]
        map2 [{:column_name "collection_id", :data_type "USER-DEFINED"} {:column_name "order", :data_type "boolean"}]]
    (keys-exist?2 map2 keys {:foo "bar"})))

(comment
  (let [;; works / OK
        ;keys (vector "collection_id" "order" "creator_id" )
        keys (vector "collection_id") ;; wrong
        keys (vector "institution") ;; ok

        map2 (clojure.lang.PersistentHashMap/create {:institution "Institution"
                                                     :institutional_id "ID"
                                                     :name "Name"
                                                     :type "Type"
                                                     :institutional_name "Institutional Name"
                                                     :created_at (java.util.Date.)})]
    (keys-exist?2 map2 keys {:foo "bar"})))

(defn validate-keys [table-name db-meta wl-attr bl-attr context]
  (let [debug-info {:context context
                    :table-name table-name
                    :db-meta db-meta}

        wl-validation (when wl-attr
                        (keys-exist? db-meta wl-attr (assoc debug-info :validation-type ":wl / whitelist")))

        bl-validation (when bl-attr
                        (keys-exist? db-meta bl-attr (assoc debug-info :validation-type ":bl / blacklist")))]

    ;(when (or wl-attr bl-attr)
    ;  (println ">o> !!!!!!!!!!! VALIDATION:" wl-validation " / " bl-validation))
    ))
(defn create-raw-schema [data]
  (let [raw (data :raw)
        raw-schema-name (data :raw-schema-name)
        result []

        res (reduce
             (fn [acc item]
               (reduce (fn [inner-acc [key value]]
                         (cond (not (str/starts-with? (name key) "_"))
                               (let [table-name key
                                     wl-attr (:wl value)
                                     bl-attr (:bl value)
                                     rename-attr (:rename value)
                                     key (name key)
                                     db-meta (fetch-table-metadata key)

                                     _ (slog (str "[handle not-additional] \n
                                  table-name=" table-name "\n
                                  wl-attr=" wl-attr "\n
                                  bl-attr=" bl-attr "\n
                                  rename-attr=" rename-attr "\n
                                  key=" key "\n
                                  db-data=" db-meta "\n"))

                                      ;debug-info {
                                      ;            ;:table table-name
                                      ;            ;:wl-attr wl-attr
                                      ;            ;:bl-attr bl-attr
                                      ;            ;:rename-attr rename-attr
                                      ;            :context "RAW-DEFINITION"
                                      ;            :table-name key
                                      ;            :db-meta db-meta}
                                      ;
                                      ;;; Check if all keys exist
                                      ;wl-validation (if (not (nil? wl-attr)) (keys-exist? db-meta wl-attr (assoc debug-info :validation-type ":wl / whitelist")))
                                      ;
                                      ;;; Check if any key does not exist
                                      ;bl-validation (if (not (nil? bl-attr)) (keys-exist? db-meta bl-attr (assoc debug-info :validation-type ":bl / blacklist")))
                                      ;
                                      ;_ (when-not (and (nil? wl-attr) (nil? bl-attr))
                                      ;    (println ">o> !!!!!!!!!!! VALIDATION:" wl-validation " / " bl-validation))

                                     db-meta (if (nil? rename-attr)
                                               db-meta
                                               (rename-column-names db-meta rename-attr))

                                     _ (validate-keys table-name db-meta wl-attr bl-attr "RAW-DEFINITION")

                                     res-wl-bl (cond
                                                 (not (nil? bl-attr)) (remove-maps-by-entry-values db-meta :column_name bl-attr)
                                                 (not (nil? wl-attr)) (keep-maps-by-entry-values db-meta :column_name wl-attr)
                                                 :else db-meta)
                                     res3 (postgres-cfg-to-schema table-name res-wl-bl)]
                                 (into inner-acc res3))

                               (= (name key) "_additional") (let [table-name key
                                                                  res2 (postgres-cfg-to-schema table-name value)]
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

(defn fetch-value-by-key
  [maps key]
  (some (fn [m] (get m key)) maps))

(defn revise-schema-types [table-name column_name column_type type-spec types key-types value-types]
  (let [val (fetch-value-by-key types column_name)
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

        _ (slog (str "[revise-schema-types] <<<<<<<<<<<<<<< before <<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n
        type-mapping-key=" type-mapping-key "\n
        column_name=" column_name "\n
        key-type=" key-type "\n
        value-type=" value-type "\n"))

        ;; TODO: quiet helpful for debugging
        ;_ (slog (str ">>o> !!! [set-schema] =>> " {keySection valueSection}))]
        _ (slog "??? [set-schema] =>> " {keySection valueSection})]
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
  (let [schema-def (:schemas data)
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
                               cache-as-attr (:cache-as value)

                               types-attr (:types value)
                               key-types-attr (:key-types value)
                               value-types-attr (:value-types value)

                               p (println ">o> ???? schema-raw=" schema-raw "\n
                                table-name=" table-name "\n
                                wl-attr=" wl-attr "\n
                                bl-attr=" bl-attr "\n
                                cache-as-attr=" cache-as-attr "\n
                                types-attr=" types-attr "\n
                                key-types-attr=" key-types-attr "\n
                                  value-types-attr=" value-types-attr "\n")

                               _ (validate-keys table-name schema-raw wl-attr bl-attr "SCHEMAS-DEFINITION")

                               result-bl-wl (cond
                                              (not (nil? bl-attr)) (remove-by-keys schema-raw bl-attr)
                                              (not (nil? wl-attr)) (keep-by-keys schema-raw wl-attr)
                                              :else schema-raw)

                               result (process-revision-of-schema-types table-name result-bl-wl types-attr key-types-attr value-types-attr)

                               _ (set-schema (keyword key) result)
                               _ (when (not (nil? cache-as-attr))
                                   (doseq [kw cache-as-attr]
                                     (set-schema (keyword kw) result)))]

                           (into inner-acc result)))
                       acc item))
             result schema-def)] res))

(defn create-dynamic-schema [cfg-array]
  (doseq [c cfg-array]
    (let [_ (create-raw-schema c)
          _ (when (contains? c :schemas)
              (create-schemas-by-config c))])))
