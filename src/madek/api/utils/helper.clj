(ns madek.api.utils.helper
  (:require [clojure.tools.logging :as logging]
            [pghstore-clj.core :refer [to-hstore]])
  (:import (java.util UUID)))

; [madek.api.utils.helper :refer [t]]
(defn t [s] (str s ".. MANUALLY TESTED"))

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
       value))))

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


(defn urls-to-custom-format [urls]
  (let [
        transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))

; [madek.api.utils.helper :refer [convert-map]]
(defn convert-map [map]
  (-> map
      ;(update :external_uris #(
      ;                          if (contains? map :external_uris)
      ;                          (if (nil? (:external_uris map))
      ;                            ([:raw "'{}'"])
      ;                            ([:raw (:external_uris map)])
      ;                            )
      ;                          ([:raw "'{}'"])
      ;                                                            ))

      ;(update :external_uris #(if (and (contains? map :external_uris) (nil? (:external_uris map)))
      ;(update :external_uris #(if  (nil? %)
      ;                               [:raw "'{}'"]
      ;                          [:raw (str (set %))]
      ;                               ))


      (update :external_uris #(if  (nil? %)
                                [:raw "'{}'"]
                                ( urls-to-custom-format %)))

      ;(update :external_uris (fn [uris] (if (nil? uris)
      ;                                        [:raw "'{}'"]
      ;                                        ;[:raw (str "'" (set uris) "'")])))
      ;                                        [:raw (str  (set uris) )])))


      ;(update :external_uris (fn [uris]
      ;
      ;                         [:raw (str (set (if (nil? uris)
      ;                                          "'{}'"
      ;                                          uris)))
      ;                                          ;(format-uris uris))))
      ;                         ]
      ;                         ))



      (update :creator_id #(if (contains? map :creator_id) (to-uuid % :creator_id)))

      ;(update :external_uris #(if (contains? map :external_uris) ([:cast % :varchar])))
      ;(update :start_date #(if (contains? map :start_date) (format-date %)))
      ;(update :end_date #(if (contains? map :end_date) (format-date %)))
      ;(update :inspection_start_date #(if (contains? map :inspection_start_date) (format-date %)))
      ;(update :updated_at #(if (contains? map :updated_at) (format-date %)))
      ))


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



