(ns madek.api.utils.helper
  (:import (java.util UUID)))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid [id] (if (instance? String id) (UUID/fromString id) id))

; [madek.api.utils.helper :refer [to-uuids]]
(defn to-uuids [ids]
  (map (fn [id] (if (instance? String id) (UUID/fromString id) id)) ids))

; [madek.api.utils.helper :refer [merge-query-parts]]
(defn merge-query-parts [query-parts]
  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
        required-entries (- (count query-parts) placeholder-count)
        merged (vector (apply str (interpose " " (take required-entries query-parts))))
        remaining (drop required-entries query-parts)]
    (concat merged remaining)))