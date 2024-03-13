(ns madek.api.resources.vocabularies.index
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.vocabularies.permissions :as permissions]
   [madek.api.utils.helper :refer [str-to-int]]
   [next.jdbc :as jdbc]))

(defn- where-clause
  [user-id]
  (let [vocabulary-ids (permissions/accessible-vocabulary-ids user-id)]
    (if (empty? vocabulary-ids)
      [:= :vocabularies.enabled_for_public_view true]
      [:or
       [:= :vocabularies.enabled_for_public_view true]
       [:in :vocabularies.id vocabulary-ids]])))

(defn- base-query
  [user-id size offset]
  (-> (sql/select :*)                                       ;:id)
      (sql/from :vocabularies)
      (sql/where (where-clause user-id))
      (sql/offset offset)
      (sql/limit size)
      sql-format))

(defn- query-index-resources [request]
  (let [user-id (-> request :authenticated-entity :id)
        qparams (-> request :query-params)

        p (println ">o> qparams1" qparams)
        p (println ">o> qparams2" (-> request :path-params))

        page (get qparams "page")
        count (get qparams "count")

        offset (str-to-int page 1)
        size (str-to-int count 5)

        p (println ">o> offset" offset ", size" size)

        query (base-query user-id size offset)
        p (println ">o> query" query)
        ]
    ;(logging/info "query-index-resources: " query)
    (jdbc/execute! (get-ds) query)))

(defn transform_ml [vocab]
  (assoc vocab
         :labels (sd/transform_ml (:labels vocab))
         :descriptions (sd/transform_ml (:descriptions vocab))))

(defn get-index [request]
  (catcher/with-logging {}
    (let [db-result (query-index-resources request)

          p (println ">o> db-result" db-result)

          result (->> db-result
                   (map transform_ml)
                   (map sd/remove-internal-keys))]
      (sd/response_ok {:vocabularies result}))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
