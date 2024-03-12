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
  ([s text] (str s " / ToFix: " text))
  )


; [madek.api.utils.helper :refer [str-to-int]]
(defn str-to-int
  "Attempts to convert a string to an integer, returning a default value if conversion fails."
  [str default-value]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException e
      default-value)))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid
  ([value]
   (try
     (let [result (if (instance? String value) (UUID/fromString value) value)]
       ; success path, result is available for further processing if needed
       result)
     (catch Exception e
       ; catch block, log the error and return id as the error handling result
       (logging/warn ">>> ERROR in to-uuid[id], id=" value ", exception=" (.getMessage e))
       value)))

  ([value key]
  ; ;(def keys-to-cast-to-uuid [:media_entry_id :media_file_id :preview_id :media_resource_id :media_file])
  ; ;(def keys-to-cast-to-uuid [])
   (def keys-to-cast-to-uuid [:user_id :id])

   ;(println (str "??? INFO / CAST TO UUID, id=" id ", key=" key ", keys\"=" keys-to-cast-to-uuid))

   (try
     (if (:and (contains? keys-to-cast-to-uuid key) (instance? String value))
       (UUID/fromString value)
       value)
     (catch Exception e
       (logging/warn ">>> ERROR2 in to-uuid[id], id=" value ", key=" key " exception=" (.getMessage e))
       value)))

  ([value key table]
   (def keys-to-cast-to-uuid [:user_id :id])

   (if (= (name table) "meta_keys")
     value
     (to-uuid value key)))




   ;(try
   ;  (if (:and (contains? keys-to-cast-to-uuid key) (instance? String value))
   ;    (UUID/fromString value)
   ;    value)
   ;  (catch Exception e
   ;    (logging/warn ">>> ERROR2 in to-uuid[id], id=" value ", key=" key " exception=" (.getMessage e))
   ;    value))
)


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
(defn convert-to-raw-set [urls]
  (let [
        transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))


(defn convert-to-raw-array [urls]
  (let [
        transformed-urls urls
        combined-str (str "' [" (clojure.string/join "," transformed-urls) "]'")]
    [:raw combined-str]))

; [madek.api.utils.helper :refer [convert-map]]
(defn convert-map [map]
  (-> map
      (update :external_uris #(if  (nil? %)
                                [:raw "'{}'"]
                                ( convert-to-raw-set %)))         ;;rename to convert-to-raw-set

      (update :creator_id #(if (contains? map :creator_id) (to-uuid % :creator_id)))

      ;(update :external_uris #(if (contains? map :external_uris) ([:cast % :varchar])))
      ;(update :start_date #(if (contains? map :start_date) (format-date %)))
      ;(update :end_date #(if (contains? map :end_date) (format-date %)))
      ;(update :inspection_start_date #(if (contains? map :inspection_start_date) (format-date %)))
      ;(update :updated_at #(if (contains? map :updated_at) (format-date %)))
      ))

; [madek.api.utils.helper :refer [convert-map-if-exist]]
;(defn convert-map-if-exist [map]
;  (-> map
;      map (if (contains? map :external_uris) (update :external_uris #(if (nil? %)
;                                [:raw "'{}'"]
;                                (convert-uris %)))
;          map
;          )  ;;rename to convert-to-raw-set
;      ;
;      (update :allowed_people_subtypes #(if (nil? %)
;                                          [:raw "'[]'"]
;                                          (convert-to-raw-array %)))
;
;      (update :creator_id #(if (contains? map :creator_id)
;                             (to-uuid % :creator_id)
;                             %))
;
;
;      )
;    )


(defn modify-if-exists [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn convert-map-if-exist [m]
  (-> m
      (modify-if-exists :external_uris #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :creator_id #(if (contains? m :creator_id) (to-uuid % :creator_id)))
      (modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))))
      ;(modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-array %)))))





(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [

        map {:external_uris "{mein/link/78}"}
        map {:external_uris "{mein/link/78}"}
        map {:external_uris ["test/me/now/78"]}

        res (convert-map map)
        ]
    res
    )

  )


; [madek.api.utils.helper :refer [cast-to-hstore]]
(defn cast-to-hstore [data]
  (let [keys [:labels :descriptions :hints :documentation_urls]]
    (reduce (fn [acc key]
              (if (contains? acc key)
                (let [field-value (get acc key)
                      transformed-value (to-hstore field-value)] ; Assume to-hstore is defined elsewhere
                  (assoc acc key transformed-value))
                acc))
            data
            keys)))

(defn array-to-map [arr]
  (zipmap arr (range (count arr)))) (defn array-to-map [arr]
                                      (zipmap arr (range (count arr))))

(defn map-to-array [m]
  (map first (sort-by val m)))

;; =================================================================

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


