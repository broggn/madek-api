(ns madek.api.utils.helper

  (:require [clojure.set :refer [subset?]]
            [taoensso.timbre :refer [debug error info spy warn]])
  (:import (java.time ZonedDateTime)
           (java.time ZoneId)
           (java.time.format DateTimeFormatter)
           (java.util UUID)))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid [id] (if (instance? String id) (UUID/fromString id) id))

; [madek.api.utils.helper :refer [to-uuids]]
(defn to-uuids [ids]
  (map (fn [id] (if (instance? String id) (UUID/fromString id) id)) ids))
