(ns madek.api.db.dynamic_schema.common
  (:require
   [madek.api.db.dynamic_schema.schema_logger :refer [slog]]
   [schema.core :as s]
   ))

(def schema-cache (atom {}))
(def enum-cache (atom {}))

(defn get-schema [key & [default]]
  (let [val (or (get @schema-cache key default) s/Any)]
    (slog (str "[get-schema] " key "=" val))
    val))

(defn set-schema [key value]
  (slog (str "[set-schema] (" key ") ->" value))
  (swap! schema-cache assoc key (into {} value)))

(defn get-enum [key & [default]]
  (let [val (get @enum-cache key default)] val))

(defn set-enum [key value]
  (swap! enum-cache assoc key value))
