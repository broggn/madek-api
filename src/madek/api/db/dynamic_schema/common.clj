(ns madek.api.db.dynamic_schema.common
  (:require
   [madek.api.db.dynamic_schema.schema_logger :refer [slog]]
   [schema.core :as s]

   [taoensso.timbre :refer [debug info warn error errorf]])
  )



(def schema-cache (atom {}))
(def enum-cache (atom {}))
(def validation-cache (atom []))

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

(defn get-validation-cache []
  @validation-cache)

(defn add-to-validation-cache [new-element]
  (error "[add-to-validation-cache]" new-element)
  ;(errorf "2[add-to-validation-cache]" new-element)
  ;(error "3[add-to-validation-cache] wtf?????????" {})
  ;(warn "4[add-to-validation-cache] wtf?????????" {})

  (swap! validation-cache conj new-element))