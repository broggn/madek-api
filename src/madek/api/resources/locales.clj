(ns madek.api.resources.locales
  (:require
   [clojure.tools.logging :as logging]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.utils.config :refer [get-config]]
   [next.jdbc :as jdbc]))

(defn- find-app-setting
  []
  (let [query (-> (sql/select :*)
                  (sql/from :app_settings)
                  (sql-format))]
    (jdbc/execute-one! (get-ds) query)))

(defn- default-locale
  []
  (let [app-setting (find-app-setting)]
    (if-not (nil? app-setting)
      (:default_locale app-setting)
      (let [config (get-config)]
        (:madek_default_locale config)))))

(defn add-field-for-default-locale
  [field-name result]
  (let [field-plural (keyword (str field-name "s"))
        field-name (keyword field-name)]
    (assoc result field-name (get-in result [field-plural (default-locale)]))))
