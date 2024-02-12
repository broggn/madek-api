(ns madek.api.resources.vocabularies.index
  (:require
   [clojure.java.jdbc :as jdbco]
   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]

       [taoensso.timbre :refer [info warn error spy]]
           [logbug.debug :as debug]


   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]

               [madek.api.db.core :refer [get-ds]]

   [madek.api.resources.shared :as sd]
   [madek.api.resources.vocabularies.permissions :as permissions]
   [madek.api.utils.sql :as sqlo]))

(defn- where-clause
  [user-id]
  (let [vocabulary-ids (permissions/accessible-vocabulary-ids user-id)]
    (if (empty? vocabulary-ids)
      [:= :vocabularies.enabled_for_public_view true]
      [:or
       [:= :vocabularies.enabled_for_public_view true]
       [:in :vocabularies.id (spy vocabulary-ids)]])))

(defn- base-query
  [user-id]
  (-> (sql/select :*);:id)
      (sql/from :vocabularies)
      (sql/where (where-clause user-id))
      sql-format))

(defn- query-index-resources [request]
  (let [user-id (-> request :authenticated-entity :id)
        query (base-query user-id)]
    ;(logging/info "query-index-resources: " query)
    (jdbc/execute! (get-ds) query)))

(defn transform_ml [vocab]
  (assoc vocab
         :labels (sd/transform_ml (:labels vocab))
         :descriptions (sd/transform_ml (:descriptions vocab))))

(defn get-index [request]
  (catcher/with-logging {}
    (let [db-result (query-index-resources request)
          result (->> db-result
                      (map transform_ml)
                      (map sd/remove-internal-keys))]
      (sd/response_ok {:vocabularies result}))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
