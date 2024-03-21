(ns madek.api.resources.vocabularies.index
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]
   [clojure.string :as str]


   [logbug.debug :as debug]


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
  ([user-id size offset]
   (-> (sql/select :*)                                      ;:id)
       (sql/from :vocabularies)
       (sql/where (where-clause user-id))
       (sql/offset offset)
       (sql/limit size)
       sql-format))

  ([user-id size offset request]

   (let [
         is_admin (-> request :is_admin)
         is_admin_endpoint (str/includes? (-> request :uri) "/admin/")

         select (if is_admin_endpoint
                  (sql/select :*)
                  (sql/select :admin_comment :position :labels :descriptions))
         ]

     (-> select
         (sql/from :vocabularies)
         (sql/where (where-clause user-id))
         (sql/offset offset)
         (sql/limit size)
         sql-format)))

  )



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

        query (base-query user-id size offset request)
        p (println ">o> query" query)]
    ;(logging/info "query-index-resources: " query)
    (jdbc/execute! (get-ds) query)))

(defn transform_ml [vocab]
  (assoc vocab
         :labels (sd/transform_ml (:labels vocab))
         :descriptions (sd/transform_ml (:descriptions vocab))))

(defn get-index [request]
  (catcher/with-logging {}
    (let [

          db-result (query-index-resources request)

          is_admin (-> request :is_admin)

          is_admin_endpoint (str/includes? (-> request :uri) "/admin/")

          uri (-> request :uri)



          p (println ">o> isAdmin?" (-> request :is_admin))

          p (println ">o> db-result" db-result)
          p (println ">o> uri" uri)
          p (println ">o> is_admin_endpoint" is_admin_endpoint)

          ;;;; TODO: BROKEN
          ;result (->> db-result
          ;         sd/transform_ml)

          ; iterate over result and process sd/transform_ml for each element
           result (map transform_ml db-result)


          p (println ">o> result" result)

          ]
      (sd/response_ok {:vocabularies result})

      ))

  )

;### Debug ####################################################################
(debug/debug-ns *ns*)
