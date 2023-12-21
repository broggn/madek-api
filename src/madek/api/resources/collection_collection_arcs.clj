(ns madek.api.resources.collection-collection-arcs
  (:require
   [clojure.java.jdbc :as jdbc]
   [logbug.catcher :as catcher]
   [madek.api.pagination :as pagination]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sql]
   [reitit.coercion.schema]
   [schema.core :as s]))


(defn arc-query [request]
  (-> (sql/select :*)
      (sql/from :collection_collection_arcs)
      (sql/merge-where [:= :id (-> request :parameters :path :id)])
      sql/format))

(defn handle_get-arc [request]
  (let [query (arc-query request)
        db-result (jdbc/query (rdbms/get-ds) query)]
    (if-let [arc (first db-result)]
      (sd/response_ok arc)
      (sd/response_failed "No such collection-collection-arc" 404))))

(defn arc-query-by-parent-and-child [request]
  (-> (sql/select :*)
      (sql/from :collection_collection_arcs)
      (sql/merge-where [:= :parent_id (-> request :parameters :path :collection_id)])
      (sql/merge-where [:= :child_id (-> request :parameters :path :child_id)])
      sql/format))

(defn handle_arc-by-parent-and-child [request]
  (let [query (arc-query-by-parent-and-child request)
        db-result (jdbc/query (rdbms/get-ds) query)]
    (if-let [arc (first db-result)]
      (sd/response_ok arc)
      (sd/response_failed "No such collection-collection-arc" 404))))

(defn arcs-query [query-params]
  (-> (sql/select :*)
      (sql/from :collection_collection_arcs)
      (sd/build-query-param query-params :child_id)
      (sd/build-query-param query-params :parent_id)
      (pagination/add-offset-for-honeysql query-params)
      sql/format))

(defn handle_query-arcs [request]
  (let [query (arcs-query (-> request :parameters :query))
        db-result (jdbc/query (get-ds) query)]
    (sd/response_ok {:collection-collection-arcs db-result})))

(defn handle_create-col-col-arc [req]
  (try
    (catcher/with-logging {}
      (let [parent-id (-> req :parameters :path :collection_id)
            child-id (-> req :parameters :path :child_id)
            data (-> req :parameters :body)
            ins-data (assoc data :parent_id parent-id :child_id child-id)]
        (if-let [ins-res (first (jdbc/insert! (rdbms/get-ds)
                                              :collection_collection_arcs
                                              ins-data
                                              {:entities (jdbc/quoted \")}))]
          (sd/response_ok ins-res)
          (sd/response_failed "Could not create collection-collection-arc" 406))))
    (catch Exception e (sd/response_exception e))))

(defn- sql-cls-update [parent-id child-id]
  (-> (sql/where [:= :parent_id parent-id]
                 [:= :child_id child-id])
      (sql/format)
      (update-in [0] #(clojure.string/replace % "WHERE" ""))))

(defn handle_update-arc [req]
  (try
    (catcher/with-logging {}
      (let [parent-id (-> req :parameters :path :collection_id)
            child-id (-> req :parameters :path :child_id)
            data (-> req :parameters :body)
            whcl (sql-cls-update parent-id child-id)
            result (jdbc/update! (rdbms/get-ds)
                                 :collection_collection_arcs
                                 data whcl
                                 {:entities (jdbc/quoted \")})]
        (if (= 1 (first result))
          (sd/response_ok (sd/query-eq-find-one
                           :collection_collection_arcs
                           :parent_id parent-id
                           :child_id child-id))
          (sd/response_failed "Could not update collection collection arc." 406))))
    (catch Exception e (sd/response_exception e))))

(defn handle_delete-arc [req]
  (try
    (catcher/with-logging {}
      (let [parent-id (-> req :parameters :path :collection_id)
            child-id (-> req :parameters :path :child_id)
            olddata (sd/query-eq-find-one
                     :collection_collection_arcs
                     :parent_id parent-id
                     :child_id child-id)
            delquery (sql-cls-update parent-id child-id)
            delresult (jdbc/delete! (get-ds) :collection_collection_arcs delquery)]
        (if (= 1 (first delresult))
          (sd/response_ok olddata)
          (sd/response_failed "Could not delete collection collection arc." 406))))
    (catch Exception e (sd/response_exception e))))

(def schema_collection-collection-arc-export
  {:id s/Uuid
   :parent_id s/Uuid
   :child_id s/Uuid
   :highlight (s/maybe s/Bool)
   :order (s/maybe s/Num)
   :position (s/maybe s/Int)
   :created_at s/Any
   :updated_at s/Any})

(def schema_collection-collection-arc-update
  {;(s/optional-key :id) s/Uuid
   ;(s/optional-key :parent_id) s/Uuid
   ;(s/optional-key :child_id) s/Uuid
   (s/optional-key :highlight) s/Bool
   (s/optional-key :order) s/Num
   (s/optional-key :position) s/Int
   ;(s/optional-key :created_at) s/Any
   ;(s/optional-key :updated_at) s/Any
   })
(def schema_collection-collection-arc-create
  {;(s/optional-key :id) s/Uuid
   ;(s/optional-key :parent_id) s/Uuid
   ;(s/optional-key :child_id) s/Uuid

   (s/optional-key :highlight) s/Bool
   (s/optional-key :order) s/Num
   (s/optional-key :position) s/Int})

; TODO add permission checks
(def ring-routes
  ["/collection-collection-arcs"
   ["/"
    {:get
    ; no permission checks
     {:summary (sd/sum_pub "Query collection collection arcs.")
      :handler handle_query-arcs
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :child_id) s/Uuid
                           (s/optional-key :parent_id) s/Uuid
                           (s/optional-key :page) s/Int
                           (s/optional-key :count) s/Int}}
      :responses {200 {:body {:collection-collection-arcs [schema_collection-collection-arc-export]}}}}}]

   ; no permission checks
   ["/:id"
    {:get
     {:summary (sd/sum_pub "Get collection collection arcs.")
      :handler handle_get-arc
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_collection-collection-arc-export}
                  404 {:body s/Any}}}}]])

; TODO rename param use middleware for permissions
(def collection-routes
  ["/collection/:collection_id"
   ;["/collection-arcs"
   ; {:get
   ;  {:summary "List collection collection arcs."
   ;   :handler arcs
   ;   
   ;   :coercion reitit.coercion.schema/coercion
   ;   :parameters {:path {:parent_id s/Uuid}}
   ;   :responses {200 {:body s/Any}} ; TODO response coercion
   ;   }
   ; }
   ;]
   ["/collection-arc/:child_id"
    {:post
     {:summary (sd/sum_usr "Create collection collection arc")
      :handler handle_create-col-col-arc
      :middleware [sd/ring-wrap-add-media-resource
                   sd/ring-wrap-authorization-edit-metadata]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:collection_id s/Uuid
                          :child_id s/Uuid}
                   :body schema_collection-collection-arc-create}
      :responses {200 {:body schema_collection-collection-arc-export}
                  406 {:body s/Any}}}

     :get
     {:summary (sd/sum_usr "Get collection collection arc.")
      :handler handle_arc-by-parent-and-child
      :middleware [sd/ring-wrap-add-media-resource
                   sd/ring-wrap-authorization-view]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:collection_id s/Uuid
                          :child_id s/Uuid}}
      :responses {200 {:body schema_collection-collection-arc-export}
                  404 {:body s/Any}}}

     :put
     {:summary (sd/sum_usr "Update collection collection arc")
      :handler handle_update-arc
      :middleware [sd/ring-wrap-add-media-resource
                   sd/ring-wrap-authorization-edit-metadata]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:collection_id s/Uuid
                          :child_id s/Uuid}
                   :body schema_collection-collection-arc-update}
      :responses {200 {:body schema_collection-collection-arc-export}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_usr "Delete collection collection arc")
      :handler handle_delete-arc
      :middleware [sd/ring-wrap-add-media-resource
                   sd/ring-wrap-authorization-edit-metadata]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:collection_id s/Uuid
                          :child_id s/Uuid}}
      :responses {200 {:body schema_collection-collection-arc-export}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
