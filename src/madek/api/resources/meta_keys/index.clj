(ns madek.api.resources.meta-keys.index
  (:require
   [clojure.tools.logging :as logging]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.pagination :refer [add-offset-for-honeysql]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.shared]
   [madek.api.resources.vocabularies.permissions :as permissions]
   [next.jdbc :as jdbc]))

(defn- where-clause
  [user-id scope]
  (let [vocabulary-ids (permissions/accessible-vocabulary-ids user-id scope)
        perm-kw (keyword (str "vocabularies.enabled_for_public_" scope))]
    (logging/info "vocabs where clause: " vocabulary-ids " for user " user-id " and " scope)
    (if (empty? vocabulary-ids)
      [:= perm-kw true]
      [:or
       [:= perm-kw true]
       [:in :vocabularies.id vocabulary-ids]])))
      ;[:= :vocabularies.enabled_for_public_view true]
      ;[:or
      ;  [:= :vocabularies.enabled_for_public_view true]
      ;  [:in :vocabularies.id vocabulary-ids]])))

(defn- base-query
  [user-id scope]
  (-> (sql/select :meta_keys.*)
      (sql/from :meta_keys)
      (sql/join :vocabularies
                [:= :meta_keys.vocabulary_id :vocabularies.id])
      (sql/where (where-clause user-id scope))))

(defn get-pagination-params [request]
  (let [qparams (-> request :parameters :query)
        query-params (-> request :query-params)
        params (if (or (contains? qparams :page) (contains? qparams :count))
                 qparams
                 query-params)
        ]params))

(defn- build-query [request]
  (let [qparams (-> request :parameters :query)
        pagination-params (get-pagination-params request)
        scope (or (:scope qparams) "view")
        user-id (-> request :authenticated-entity :id)]
    (-> (base-query user-id scope)
        (sd/build-query-param qparams :vocabulary_id)
        (sd/build-query-param-like qparams :id :meta_keys.id)
        (sd/build-query-param qparams :meta_datum_object_type)
        (sd/build-query-param qparams :is_enabled_for_collections)
        (sd/build-query-param qparams :is_enabled_for_media_entries)
        (sql/order-by :meta_keys.id)
        (add-offset-for-honeysql pagination-params)
        sql-format)))

(defn db-query-meta-keys [request]
  (catcher/with-logging {}
    (let [query (build-query request)]
      (logging/info "db-query-meta-keys: query: " query)
      (jdbc/execute! (get-ds) query))))

;(defn get-index [request]
;  (catcher/with-logging {}
;    {:body
;     {:meta-keys
;      (query-index-resources request)}}))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
