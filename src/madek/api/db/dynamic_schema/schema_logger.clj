(ns madek.api.db.dynamic_schema.schema_logger
  (:require
   [taoensso.timbre :refer [info]]))

(def ENABLE_SCHEMA_LOGGING true)

(defn slog [msg]
  (if ENABLE_SCHEMA_LOGGING
    (info (str msg "\n")
          nil)))