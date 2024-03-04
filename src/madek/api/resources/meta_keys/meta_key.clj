(ns madek.api.resources.meta-keys.meta-key
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   ;[madek.api.utils.sql :as sql]

   [madek.api.resources.locales :refer [add-field-for-default-locale]]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.config :as config :refer [get-config]]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

(defn add-fields-for-default-locale
  [result]
  (add-field-for-default-locale
   "label" (add-field-for-default-locale
            "description" (add-field-for-default-locale
                           "hint" result))))

(defn- get-io-mappings
  [id]
  (let [query (-> (sql/select :key_map, :io_interface_id)
                  (sql/from :io_mappings)
                  (sql/where [:= :io_mappings.meta_key_id id])
                  (sql-format))]
    (jdbc/execute! (get-ds) query)))

(defn- prepare-io-mappings-from
  [io-mappings]
  (let [groupped (group-by :io_interface_id io-mappings)]
    (let [io-interfaces (keys groupped)]
      (map (fn [io-interface-id] {:id io-interface-id
                                  :keys (reduce (fn [m key-map]
                                                  (conj m {:key (:key_map key-map)}))
                                                []
                                                (get groupped io-interface-id))}) io-interfaces))))

(defn include-io-mappings
  [result id]
  (let [io-mappings (prepare-io-mappings-from (get-io-mappings id))]
    (assoc result :io_mappings io-mappings)))

(defn build-meta-key-query [id]
  (-> (sql/select :*)
      (sql/from :meta-keys)
      (sql/where [:= :meta-keys.id id])
      (sql-format)))

;(defn get-meta-key [request]
;  (let [id (-> request :parameters :path :id)
;        query (build-meta-key-query id)]
;    (if-let [meta-key (first (jdbc/query (get-ds) query))]
;      (let [result (include-io-mappings
;                    (sd/remove-internal-keys
;                     (add-fields-for-default-locale meta-key)) id)]
;        (sd/response_ok result))
;      (sd/response_failed "Meta-Key could not be found!" 404))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
