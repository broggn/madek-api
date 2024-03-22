(ns madek.api.resources.collections.index
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   ;[madek.api.utils.rdbms :as rdbms]
   ;[madek.api.utils.sql :as sql]

   [madek.api.pagination :as pagination]
   [madek.api.resources.collections.advanced-filter.permissions :as permissions :refer [filter-by-query-params]]
   [madek.api.resources.shared :as sd]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

         ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

;### collection_id ############################################################

(defn- filter-by-collection-id [sqlmap {:keys [collection_id] :as query-params}]

  (println ">o> collection_id=" collection_id)
  (println ">o> collection_id.cl=" (class collection_id))
  (println ">o> map=" sqlmap)

  (cond-> sqlmap
    (seq (str collection_id))
    (-> (sql/join [:collection_collection_arcs :cca]
                  [:= :cca.child_id :collections.id])
        (sql/where [:= :cca.parent_id collection_id]))))

;### query ####################################################################

(defn ^:private base-query [full-data]
  (let [toselect (if (true? full-data)
                   (sql/select :*)
                   (sql/select :collections.id, :collections.created_at))]
    (-> toselect
        (sql/from :collections))))

(defn- set-order [query query-params]
  (if (some #{"desc"} [(-> query-params :order)])
    (-> query (sql/order-by [:collections.created_at :desc]))
    (-> query (sql/order-by [:collections.created_at :asc]))))

; TODO test query and paging
(defn- build-query [request]
  (let [


        query-params (:query-params request)
        p (println ">o> query-params" query-params)


        authenticated-entity (:authenticated-entity request)
        sql-query (-> (base-query (:full_data query-params))
                      (set-order query-params)
                      (sd/build-query-param query-params :creator_id)
                      (sd/build-query-param query-params :responsible_user_id)
                      (filter-by-collection-id query-params)
                      (permissions/filter-by-query-params query-params
                                                          authenticated-entity)
                      (pagination/add-offset-for-honeysql query-params)
                      sql-format)]
    ;(logging/info "build-query"
    ;              "\nquery\n" query-params
    ;              "\nsql query:\n" sql-query)
    sql-query))

(defn- query-index-resources [request]


  (let [
        query (build-query request)
        p (println ">o> query" query)

        res (jdbc/execute! (get-ds) query)

        p (println ">o> res" res)

        ]res)

  )

;### index ####################################################################

(defn get-index [request]
  (catcher/with-logging {}
    {:body
     {:collections
      (query-index-resources request)}}))

;### Debug ####################################################################
(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
