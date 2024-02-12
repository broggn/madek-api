(ns madek.api.utils.helper
  (:require [clojure.tools.logging :as logging])
  (:import (java.util UUID)))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid
  ([id]

   (try
     (let [result (if (instance? String id) (UUID/fromString id) id)]
       ; success path, result is available for further processing if needed
       result)
     (catch Exception e
       ; catch block, log the error and return id as the error handling result
       (logging/error ">>> ERROR in to-uuid[id], id=" id ", exception=" (.getMessage e))
       id)))

  ([id key]
   ;(def keys-to-cast-to-uuid [:media_entry_id :media_file_id :preview_id :media_resource_id :media_file])
   ;(def keys-to-cast-to-uuid [])
   (def keys-to-cast-to-uuid [:user_id :id])

   ;(println (str "??? INFO / CAST TO UUID, id=" id ", key=" key ", keys\"=" keys-to-cast-to-uuid))

   (try
     (if (:and (contains? keys-to-cast-to-uuid key) (instance? String id))
       (UUID/fromString id)
       id)
     (catch Exception e

       (logging/error ">>> ERROR2 in to-uuid[id], id=" id ", key=" key " exception=" (.getMessage e))

; catch block, log the error and return id as the error handling result
       ;(println (str ">>> ERROR2 in to-uuid[id], id=" id ", key=" key " exception=" e))
       id))))

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

; [madek.api.utils.helper :refer [convert-map]]
(defn convert-map [entry]
  (-> entry
      (update :created_at #(if (contains? entry :created_at) (to-uuid %)))
      ;(update :start_date #(if (contains? entry :start_date) (format-date %)))
      ;(update :end_date #(if (contains? entry :end_date) (format-date %)))
      ;(update :inspection_start_date #(if (contains? entry :inspection_start_date) (format-date %)))
      ;(update :updated_at #(if (contains? entry :updated_at) (format-date %)))
      ))