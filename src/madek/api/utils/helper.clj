(ns madek.api.utils.helper
  (:require [clojure.tools.logging :as logging]
            [pghstore-clj.core :refer [to-hstore]])
  (:import (java.util UUID)))

; [madek.api.utils.helper :refer [t d]]
(defn t [s] (str s ".. MANUALLY TESTED"))
(defn d [s] (str s " / doc-example"))
(defn v [s] (str s " / working validation"))
(defn fv [s] (str s " / validation FAILS"))
(defn f
  ([s] (str s " / ToFix"))
  ([s text] (str s " / ToFix: " text)))

; [madek.api.utils.helper :refer [str-to-int]]
(defn str-to-int
  "Attempts to convert a string to an integer, returning a default value if conversion fails."
  [value default-value]
  (println ">o> value=" value ", default-value=" default-value)
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException e
      default-value)))

(comment
  (let [p (println ">o> int" (class 1))

        res (str-to-int "123" 0)
        res (str-to-int 456 0)
        res (str-to-int "x456" 0)
        res (str-to-int {} 0)
        res (str-to-int [] 0)]
    res))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid
  ([value]
   (try
     (let [result (if (instance? String value) (UUID/fromString value) value)]
       result)
     (catch Exception e
       (logging/warn ">>> DEV-ERROR in to-uuid[value], value=" value ", exception=" (.getMessage e))
       value)))

  ([value key]
   (def keys-to-cast-to-uuid #{:user_id :id :person_id :accepted_usage_terms_id :delegation_id})
   (println ">o> to-uuid[key value]: " value key)
   (try
     (if (and (contains? keys-to-cast-to-uuid key) (instance? String value))
       (UUID/fromString value)
       value)
     (catch Exception e
       (logging/warn ">>> DEV-ERROR in to-uuid[value key], value=" value ", key=" key " exception=" (.getMessage e))
       value)))

  ([value key table]
   (println ">o> to-uuid[key value table]: " value key table)
   (def blacklisted-tables #{"meta_keys" "vocabularies"})

   (println ">o> keys-to-cast-to-uuid / earlyExitByTableName" table)
   (println ">o> blacklistedTables=" blacklisted-tables)

   (if (contains? blacklisted-tables (name table))
     value
     (to-uuid value key))))

(comment
  (let [;p (println "\nquery ok1" (to-uuid "123e4567-e89b-12d3-a456-426614174000" :user_id))
        ;p (println "\nquery ok1" (class (to-uuid "123e4567-e89b-12d3-a456-426614174000" :user_id)))
        ;

        k "123e4567-e89b-12d3-a456-426614174000" ;ok
        k "123e" ;error - return val
        ;k 123                                               ;ok - return val

        p (println "\nquery result=" (to-uuid k))
        p (println "\nquery class=" (class (to-uuid k)))

        ;p (println "\nquery result=" (to-uuid k :user_id))
        ;p (println "\nquery class=" (class (to-uuid k :user_id)))
        ]))

;[madek.api.utils.helper :refer [to-uuids]]
(defn to-uuids [ids] (map (fn [id] (if (instance? String id) (UUID/fromString id) id)) ids))

; [madek.api.utils.helper :refer [merge-query-parts]]
(defn merge-query-parts "DEPR" [query-parts]
  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
        required-entries (- (count query-parts) placeholder-count)
        merged (vector (apply str (interpose " " (take required-entries query-parts))))
        remaining (drop required-entries query-parts)]
    (concat merged remaining)))

(defn format-uris [uris]
  ;(clojure.string/join "" (str "{" uris "}")))

  (println ">o> format-uris =>" format-uris)

  (clojure.string/join "" (map #(str "{" % "}") uris)))

; [madek.api.utils.helper :refer [urls-to-custom-format]]
;; TODO: maybe possible with json/dump?
(defn convert-to-raw-set [urls]
  (let [transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))

(defn convert-to-raw-array [urls]
  (let [transformed-urls urls
        combined-str (str "' [" (clojure.string/join "," transformed-urls) "]'")]
    [:raw combined-str]))

; [madek.api.utils.helper :refer [convert-map]]
(defn convert-map [map]
  (-> map
      (update :external_uris #(if (nil? %)
                                [:raw "'{}'"]
                                (convert-to-raw-set %))) ;;rename to convert-to-raw-set

      (update :creator_id #(if (contains? map :creator_id) (to-uuid % :creator_id)))

      ;(update :external_uris #(if (contains? map :external_uris) ([:cast % :varchar])))
      ;(update :start_date #(if (contains? map :start_date) (format-date %)))
      ;(update :end_date #(if (contains? map :end_date) (format-date %)))
      ;(update :inspection_start_date #(if (contains? map :inspection_start_date) (format-date %)))
      ;(update :updated_at #(if (contains? map :updated_at) (format-date %)))
      ))



(defn modify-if-exists [m k f]
  (if (contains? m k)
    (update m k f)
    m))

;; Used for columns of jsonb type
; [madek.api.utils.helper :refer [convert-map-if-exist]]
(defn convert-map-if-exist [m]
  (-> m
      (modify-if-exists :id #(if (contains? m :id) (to-uuid % :id)))
      (modify-if-exists :settings #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :external_uris #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))

      (modify-if-exists :creator_id #(if (contains? m :creator_id) (to-uuid %)))
      (modify-if-exists :person_id #(if (contains? m :person_id) (to-uuid %)))
      (modify-if-exists :user_id #(if (contains? m :user_id) (to-uuid %)))
      (modify-if-exists :accepted_usage_terms_id #(if (contains? m :accepted_usage_terms_id) (to-uuid %)))

      (modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))))
;(modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-array %)))))

(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [map {:external_uris "{mein/link/78}"}
        map {:external_uris "{mein/link/78}"}
        map {:external_uris ["test/me/now/78"]}

        res (convert-map map)]
    res))

; [madek.api.utils.helper :refer [cast-to-hstore]]
(defn cast-to-hstore [data]
  (let [keys [:labels :descriptions :contents :hints :documentation_urls]]
    (reduce (fn [acc key]
              (if (contains? acc key)
                (let [field-value (get acc key)
                      transformed-value (to-hstore field-value)] ; Assume to-hstore is defined elsewhere
                  (assoc acc key transformed-value))
                acc))
            data
            keys)))

(defn array-to-map [arr]
  (zipmap arr (range (count arr))))
(defn array-to-map [arr]
  (zipmap arr (range (count arr))))

(defn map-to-array [m]
  (map first (sort-by val m)))

;; =================================================================
;; TODO: replace-java-hashmap
;; convert java.*.HashMap to ClolureMap
(defn replace-java-hashmap [v]
  (if (instance? java.util.HashMap v)
    (into {} (for [[k v] v]
               [(keyword k) v]))
    v))

; [madek.api.utils.helper :refer [replace-java-hashmaps]]
(defn replace-java-hashmaps [m]
  (reduce-kv (fn [acc k v]
               (assoc acc k (replace-java-hashmap v)))
             {}
             m))



