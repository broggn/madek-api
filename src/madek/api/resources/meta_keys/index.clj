(ns madek.api.resources.meta-keys.index
  (:require
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]

   [madek.api.pagination :refer [add-offset-for-honeysql]]


   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.shared :as shared]
   [madek.api.resources.vocabularies.permissions :as permissions]
   [madek.api.utils.helper :refer [array-to-map convert-to-raw-set map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts str-to-int]]
   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]



   ;[madek.api.utils.rdbms :as rdbms]
   [madek.api.utils.helper :refer [str-to-int]]

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
  (-> (sql/select :meta_keys.*) ; :meta_keys.id :meta_keys.vocabulary_id)
      (sql/from :meta_keys)
      (sql/join :vocabularies
                [:= :meta_keys.vocabulary_id :vocabularies.id])
      (sql/where (where-clause user-id scope))))


(defn get-pagination-params [request]


  (let [

        qparams (-> request :parameters :query)
        query-params (-> request :query-params)

        params (if (or (contains? qparams :page) (contains? qparams :count))
                 qparams
                 query-params
          )


        p (println ">o> qparams=" qparams)
        p (println ">o> query-params=" query-params)
        p (println ">o> pagination-params=" params)

        ;offset (str-to-int (params :page) 0)
        ;p (println ">o> offset" offset)
        ;p (println ">o> offset.class" (class offset))
        ;
        ;count (str-to-int (params :count) 100)
        ;
        ;params {:page offset :limit count}

        ] params ))

;(defn set-pagination [stmt request]
;  (let [pagination-params (get-pagination-params request)
;
;        query (-> stmt
;                  (sql/offset (:offset pagination-params))
;                  (sql/limit (:limit pagination-params)) ;; TODO: FIXME / TEST-IT
;                  )
;
;        ] query ))



(defn- build-query [request]
  (let [qparams (-> request :parameters :query)

        pagination-params (get-pagination-params request)


        scope (or (:scope qparams) "view")
        user-id (-> request :authenticated-entity :id)

        p (println ">o> pagination-params=" pagination-params)
        ;
        ;offset (str-to-int (pagination-params :page) 1)
        ;p (println ">o> offset" offset)
        ;p (println ">o> offset.class" (class offset))
        ;
        ;size (str-to-int (pagination-params :count) 5)
        ;p (println ">o> :size" size)
        ;p (println ">o> :size.class" (class size))


        ]

    (-> (base-query user-id scope)
        (sd/build-query-param qparams :vocabulary_id)
        (sd/build-query-param-like qparams :id :meta_keys.id)
        (sd/build-query-param qparams :meta_datum_object_type)
        (sd/build-query-param qparams :is_enabled_for_collections)
        (sd/build-query-param qparams :is_enabled_for_media_entries)

        (sql/order-by :meta_keys.id)

        (add-offset-for-honeysql pagination-params)

        ;(sql/offset (:offset pagination-params))
        ;(sql/limit (:limit pagination-params)) ;; TODO: FIXME / TEST-IT

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
