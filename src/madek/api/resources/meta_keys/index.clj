(ns madek.api.resources.meta-keys.index
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

   [madek.api.resources.shared :as sd]
   [madek.api.resources.shared :as shared]
   [madek.api.resources.vocabularies.permissions :as permissions]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

         ;[leihs.core.db :as db]
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
  (-> (sql/select :*); :meta_keys.id :meta_keys.vocabulary_id)
      (sql/from :meta_keys)
      (sql/join :vocabularies
                [:= :meta_keys.vocabulary_id :vocabularies.id])
      (sql/where (where-clause user-id scope))))

(defn- build-query [request]
  (let [qparams (-> request :parameters :query)
        scope (or (:scope qparams) "view")
        user-id (-> request :authenticated-entity :id)]
    (-> (base-query user-id scope)
        (sd/build-query-param qparams :vocabulary_id)
        (sd/build-query-param-like qparams :id :meta_keys.id)
        (sd/build-query-param qparams :meta_datum_object_type)
        (sd/build-query-param qparams :is_enabled_for_collections)
        (sd/build-query-param qparams :is_enabled_for_media_entries)
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
