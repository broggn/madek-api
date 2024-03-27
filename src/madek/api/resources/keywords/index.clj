(ns madek.api.resources.keywords.index
  (:require
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.meta-keys.meta-key :as meta-key]
   [next.jdbc :as jdbc]))

(defn get-index [meta-datum]
  (let [meta-key (first (jdbc/execute! (get-ds)
                                       (meta-key/build-meta-key-query (:meta_key_id meta-datum))))
        query-base (-> (sql/select :keywords.*)
                       (sql/from :keywords)
                       (sql/join
                        :meta_data_keywords
                        [:= :meta_data_keywords.keyword_id :keywords.id])
                       (sql/where
                        [:= :meta_data_keywords.meta_datum_id (:id meta-datum)]))
        query (sql-format (cond-> query-base
                            (:keywords_alphabetical_order meta-key)
                            (sql/order-by [:keywords.term :asc])))]
    (jdbc/execute! (get-ds) query)))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
