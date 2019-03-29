(ns madek.api.resources.vocabularies.vocabulary
  (:require
    [madek.api.utils.rdbms :as rdbms :refer [get-ds]]
    [madek.api.utils.sql :as sql]
    [madek.api.resources.shared :refer [remove-internal-keys]]
    [madek.api.resources.vocabularies.permissions :as permissions]

    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn- where-clause
  [id user-id]
  (let [public [:= :vocabularies.enabled_for_public_view true]
        id-match [:= :vocabularies.id id]]
    (if user-id
      (let [vocabulary-ids (permissions/accessible-vocabulary-ids user-id)]
        [:and
         [:or public [:in :vocabularies.id vocabulary-ids]]
         id-match])
      [:and public id-match])))

(defn build-vocabulary-query [id user-id]
  (-> (sql/select :*)
      (sql/from :vocabularies)
      (sql/where (where-clause id user-id))
      (sql/format)))

(defn get-vocabulary [request]
  (let [id (-> request :params :id)
        user-id (-> request :authenticated-entity :id)
        query (build-vocabulary-query id user-id)]
    (if-let [vocabulary (first (jdbc/query (rdbms/get-ds) query))]
      {:body (remove-internal-keys vocabulary)}
      {:status 404 :body {:message "Vocabulary could not be found!"}})))

;### Debug ####################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
