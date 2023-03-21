(ns madek.api.resources.favorite-collections
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sql]
   [reitit.coercion.schema]
   [schema.core :as s]))


(def res-req-name :favorite_collection)
(def res-table-name "favorite_collections")
(def res-col-name :collection_id)

(defn handle_list-favorite_collection
  [req]
  (let [;full-data (true? (-> req :parameters :query :full-data))
        db-result (sd/query-find-all :favorite_collections :*)]
    (logging/info "handle_list-favorite_collection" "\nresult\n" db-result)
    (sd/response_ok db-result)))

(defn handle_list-favorite_collection-by-user
  [req]
  (let [;full-data (true? (-> req :parameters :query :full-data))
        user-id (-> req :authenticated-entity :id)
        db-result (sd/query-eq-find-all "favorite_collections" "user_id" user-id)
        id-set (map :collection_id db-result)]
    (logging/info "handle_list-favorite_collection" "\nresult\n" db-result "\nid-set\n" id-set)
    (sd/response_ok {:collection_ids id-set})
    ;(if full-data (sd/response_ok db-result) (sd/response_ok {:collection_ids id-set}))
    ))


(defn handle_get-favorite_collection
  [req]
  (let [favorite_collection (-> req res-req-name)]
    ;(logging/info "handle_get-favorite_collection" favorite_collection)
    ; TODO hide some fields
    (sd/response_ok favorite_collection)))


(defn handle_create-favorite_collection
  [req]
  (let [user-id (or (-> req :user :id) (-> req :authenticated-entity :id))
        col-id (-> req :collection :id)
        data {:user_id user-id res-col-name col-id}]
    (if-let [favorite_collection (-> req res-req-name)]
      ; already has favorite_collection
      (sd/response_ok favorite_collection)
      ; create favorite_collection entry
      (if-let [ins_res (first (jdbc/insert! (rdbms/get-ds) res-table-name data))]
        ; TODO clean result
        (sd/response_ok ins_res)
        (sd/response_failed "Could not create favorite_collection." 406)))))


(defn handle_delete-favorite_collection
  [req]
  (let [favorite_collection (-> req res-req-name)
        user-id (:user_id favorite_collection)
        collection-id (res-col-name favorite_collection)]
    (if (= 1 (first (jdbc/delete! (rdbms/get-ds) res-table-name ["user_id = ? AND collection_id = ?" user-id collection-id])))
      (sd/response_ok favorite_collection)
      (logging/error "Failed delete favorite_collection "
                     "user-id: " user-id "collection-id: " collection-id))))


(defn wwrap-find-favorite_collection [send404]
  (fn [handler]
    (fn [request] 
      (sd/req-find-data2
       request handler
       :user_id :collection_id
       "favorite_collections"
       "user_id" "collection_id"
       res-req-name
       send404))))

(defn wwrap-find-favorite_collection-by-auth [send404]
  (fn [handler]
    (fn [request]
       (let [user-id (-> request :authenticated-entity :id str)
             col-id (-> request :parameters :path :collection_id str)]
         (logging/info "uid\n" user-id "col-id\n" col-id)
         (sd/req-find-data-search2
          request handler
          user-id col-id
          res-table-name
          "user_id" "collection_id"
          res-req-name
          send404)))))

(defn wwrap-find-user [param]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler param "users" "id" :user true))))

(defn wwrap-find-collection [param]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler param "collections" "id" :collection true))))

(def schema_favorite_collection_export
  {:user_id s/Uuid
   :collection_id s/Uuid
   :updated_at s/Any
   :created_at s/Any})

; TODO response coercion
; TODO docu
; TODO tests
(def favorite-routes
; favorite_collection list / query
  ; TODO query params
  ["/favorite/collections"
   {:get
    {:summary  (sd/sum_adm "List users favorite_collections.")
     :handler handle_list-favorite_collection-by-user
     :coercion reitit.coercion.schema/coercion
            ;:parameters {:query {(s/optional-key :full-data) s/Bool}}
     :responses {200 {:body {:collection_ids [s/Uuid]}}}}}])

(def collection-routes
  ["/collection/:collection_id/favorite"
   {:post {:summary (sd/sum_adm "Create favorite_collection for authed user and collection.")
           :handler handle_create-favorite_collection
           :middleware [(wwrap-find-collection :collection_id)
                        (wwrap-find-favorite_collection-by-auth false)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Uuid}}}

    :get {:summary (sd/sum_adm "Get favorite_collection for authed user and collection id.")
          :handler handle_get-favorite_collection
          :middleware [(wwrap-find-favorite_collection-by-auth true)]
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:collection_id s/Uuid}}}

    :delete {:summary (sd/sum_adm "Delete favorite_collection for authed user and collection id.")
             :coercion reitit.coercion.schema/coercion
             :handler handle_delete-favorite_collection
             :middleware [(wwrap-find-favorite_collection-by-auth true)]
             :parameters {:path {:collection_id s/Uuid}}}}])

(def admin-routes
  [
   ["/admin/favorite/collections"
    {:get
     {:summary  (sd/sum_adm "List favorite_collection users.")
      :handler handle_list-favorite_collection
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :user_id) s/Uuid
                           (s/optional-key :collection_id) s/Uuid
                           (s/optional-key :full-data) s/Bool}}
      :responses {200 {:body [schema_favorite_collection_export]}}              
      }}]
    ; edit favorite_collection
    ["/admin/favorite/collections/:collection_id/:user_id" 
     {:post {:summary (sd/sum_adm "Create favorite_collection for user and collection.")
             :handler handle_create-favorite_collection
             :middleware [(wwrap-find-user :user_id)
                          (wwrap-find-collection :collection_id)
                          (wwrap-find-favorite_collection false)]
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:user_id s/Uuid
                                 :collection_id s/Uuid}}
             :responses {200 {:body schema_favorite_collection_export}
                         404 {:body s/Any}
                         406 {:body s/Any}}}

      :get {:summary (sd/sum_adm "Get favorite_collection by user and collection id.")
            :handler handle_get-favorite_collection
            :middleware [
                         (wwrap-find-favorite_collection true)]
            :coercion reitit.coercion.schema/coercion
            :parameters {:path {:user_id s/Uuid
                                :collection_id s/Uuid}}
            :responses {200 {:body schema_favorite_collection_export}
                        404 {:body s/Any}
                        406 {:body s/Any}}}

      :delete {:summary (sd/sum_adm "Delete favorite_collection by user and collection id.")
               :coercion reitit.coercion.schema/coercion
               :handler handle_delete-favorite_collection
               :middleware [(wwrap-find-favorite_collection true)]
               :parameters {:path {:user_id s/Uuid
                                   :collection_id s/Uuid}}
               :responses {200 {:body schema_favorite_collection_export}
                           404 {:body s/Any}
                           406 {:body s/Any}}}}]
  ])
