(ns madek.api.resources.custom-urls
  (:require [clojure.tools.logging :as logging]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [logbug.catcher :as catcher]
            [madek.api.db.core :refer [get-ds]]
            [madek.api.resources.shared :as sd]
            [next.jdbc :as jdbc]

            [reitit.coercion.schema]
            [schema.core :as s]))

(defn build-query [query-params]
  (let [col-sel (if (true? (-> query-params :full_data))
                  (sql/select :*)
                  (sql/select :id, :media_entry_id, :collection_id))]
    (-> col-sel
        (sql/from :custom_urls)
        (sd/build-query-param-like query-params :id)
        (sd/build-query-param query-params :collection_id)
        (sd/build-query-param query-params :media_entry_id)
        sql-format)))

(defn handle_list-custom-urls
  [req]
  (let [db-query (build-query (-> req :parameters :query))
        db-result (jdbc/execute! (get-ds) db-query)]
    (logging/info "handle_list-custom-urls" "\ndb-query\n" db-query "\nresult\n" db-result)
    (sd/response_ok db-result)))

(defn handle_get-custom-url
  [req]
  (let [id (-> req :parameters :path :id)]
    (if-let [result (sd/query-eq-find-one :custom_urls :id id)]
      (sd/response_ok result)
      (sd/response_not_found (str "No such custom_url for id: " id)))))

(defn handle_get-custom-urls
  [req]
  (let [mr (-> req :media-resource)
        mr-type (-> mr :type)
        mr-id (-> mr :id str)
        col-name (if (= mr-type "MediaEntry") :media_entry_id :collection_id)]

    (logging/info "handle_get-custom-urls"
                  "\ntype: " mr-type
                  "\nmr-id: " mr-id
                  "\ncol-name: " col-name)
    (if-let [result (sd/query-eq-find-one :custom_urls col-name mr-id)]
      (sd/response_ok result)
      (sd/response_not_found (str "No such custom_url for " mr-type " with id: " mr-id)))))

(defn handle_create-custom-urls
  [req]
  (try
    (catcher/with-logging {}
      (let [u-id (-> req :authenticated-entity :id)
            mr (-> req :media-resource)
            mr-type (-> mr :type)
            mr-id (-> mr :id str)
            data (-> req :parameters :body)
            dwid (if (= mr-type "MediaEntry")
                   (assoc data :media_entry_id mr-id :creator_id u-id :updator_id u-id)
                   (assoc data :collection_id mr-id :creator_id u-id :updator_id u-id))
            sql (-> (sql/insert-into :custom_urls)
                    (sql/values [dwid])
                    sql-format)
            ins-res (jdbc/execute! (get-ds) sql)]

        (sd/logwrite req (str "handle_create-custom-urls"
                              "\nmr-type: " mr-type
                              "\nmr-id: " mr-id
                              "\nnew-dat: " dwid
                              "\nresult: " ins-res))

        (if-let [result (first ins-res)]
          (sd/response_ok result)
          (sd/response_failed "Could not create custom_url." 406))))
    (catch Exception ex (sd/response_exception ex))))

; TODO check if own entity or auth is admin
(defn handle_update-custom-urls
  [req]
  (try
    (catcher/with-logging {}
      (let [u-id (-> req :authenticated-entity :id)
            mr (-> req :media-resource)
            mr-type (-> mr :type)
            mr-id (-> mr :id str)
            col-name (if (= mr-type "MediaEntry") :media_entry_id :collection_id)
            data (-> req :parameters :body)
            dwid (if (= mr-type "MediaEntry")
                   (assoc data :media_entry_id mr-id :updator_id u-id)
                   (assoc data :collection_id mr-id :updator_id u-id))
            sql (-> (sql/update :custom_urls)
                    (sql/set dwid)
                    (sql/where [:= col-name mr-id])
                    sql-format)
            upd-result (jdbc/execute! (get-ds) sql)]

        (sd/logwrite req (str "handle_update-custom-urls"
                              "\nmr-type: " mr-type
                              "\nmr-id: " mr-id
                              "\nnew-data\n" dwid
                              "\nresult:\n" upd-result))

        (if (= 1 (first upd-result))
          (sd/response_ok (sd/query-eq-find-one :custom_urls col-name mr-id))
          (sd/response_failed "Could not update custom_url." 406))))
    (catch Exception ex (sd/response_exception ex))))

; TODO use wrapper? no
; TODO check if own entity or auth is admin
(defn handle_delete-custom-urls
  [req]
  (try
    (catcher/with-logging {}
      (let [u-id (-> req :authenticated-entity :id)
            mr (-> req :media-resource)
            mr-type (-> mr :type)
            mr-id (-> mr :id str)
            col-name (if (= mr-type "MediaEntry")
                       :media_entry_id
                       :collection_id)]
        (if-let [del-data (sd/query-eq-find-one :custom_urls col-name mr-id)]
          (let [sql (-> (sql/delete-from :custom_urls)
                        (sql/where [:= col-name mr-id])
                        sql-format)
                del-result (jdbc/execute! (get-ds) sql)]

            (sd/logwrite req (str "handle_delete-custom-urls"
                                  "\nmr-type: " mr-type
                                  "\nmr-id: " mr-id
                                  "\nresult: " del-result))

            (if (= 1 (first del-result))
              (sd/response_ok del-data)
              (sd/response_failed (str "Could not delete custom_url " col-name " : " mr-id) 406)))
          (sd/response_failed (str "No such custom_url " col-name " : " mr-id) 404))))
    (catch Exception ex (sd/response_exception ex))))

(def schema_create_custom_url
  {:id s/Str
   :is_primary s/Bool})

(def schema_update_custom_url
  {(s/optional-key :id) s/Str
   (s/optional-key :is_primary) s/Bool})

(def schema_export_custom_url
  {:id s/Str
   :is_primary s/Bool
   :creator_id s/Uuid
   :updator_id s/Uuid
   :updated_at s/Any
   :created_at s/Any
   :media_entry_id (s/maybe s/Uuid)
   :collection_id (s/maybe s/Uuid)})

; TODO custom urls response coercion
(def query-routes
  ["/custom_urls"
   ["/"
    {:get {:summary (sd/sum_usr "Query and list custom_urls.")
           :handler handle_list-custom-urls
           :coercion reitit.coercion.schema/coercion
           :parameters {:query {(s/optional-key :full_data) s/Bool
                                (s/optional-key :id) s/Str
                                (s/optional-key :media_entry_id) s/Uuid
                                (s/optional-key :collection_id) s/Uuid}}}}]
   ["/:id"
    {:get {:summary (sd/sum_usr "Get custom_url.")
           :handler handle_get-custom-url
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Str}}}}]])

; TODO Q? custom_url without media-entry or collection ?? filter_set ?? ignore ??

(def media-entry-routes
  ["/media-entry/:media_entry_id/custom_url"
   {:get {:summary "Get custom_url for media entry."
          :handler handle_get-custom-urls
          :middleware [sd/ring-wrap-add-media-resource
                       sd/ring-wrap-authorization-view]
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:media_entry_id s/Str}}
          :responses {200 {:body schema_export_custom_url}
                      404 {:body s/Any}}}
    ; TODO db schema allows multiple entries for multiple users
    :post {:summary (sd/sum_usr "Create custom_url for media entry.")
           :handler handle_create-custom-urls
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-edit-metadata]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:media_entry_id s/Str}
                        :body schema_create_custom_url}
           :responses {200 {:body schema_export_custom_url}
                       406 {:body s/Any}}}

    :put {:summary (sd/sum_usr "Update custom_url for media entry.")
          :handler handle_update-custom-urls
          :middleware [sd/ring-wrap-add-media-resource
                       sd/ring-wrap-authorization-edit-metadata]
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:media_entry_id s/Str}
                       :body schema_update_custom_url}
          :responses {200 {:body schema_export_custom_url}
                      406 {:body s/Any}}}

    :delete {:summary (sd/sum_todo "Delete custom_url for media entry.")
             :handler handle_delete-custom-urls
             :middleware [sd/ring-wrap-add-media-resource
                          sd/ring-wrap-authorization-edit-metadata]
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:media_entry_id s/Str}}
             :responses {200 {:body schema_export_custom_url}
                         404 {:body s/Any}}}}])

(def collection-routes
  ["/collection/:collection_id/custom_url"
   {:get {:summary "Get custom_url for collection."
          :handler handle_get-custom-urls
          :middleware [sd/ring-wrap-add-media-resource
                       sd/ring-wrap-authorization-view]
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:collection_id s/Str}}}

    :post {:summary (sd/sum_usr "Create custom_url for collection.")
           :handler handle_create-custom-urls
           :middleware [sd/ring-wrap-add-media-resource
                        sd/ring-wrap-authorization-edit-metadata]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:collection_id s/Str}
                        :body schema_create_custom_url}
           :responses {200 {:body schema_export_custom_url}
                       406 {:body s/Any}}}

    :put {:summary (sd/sum_usr "Update custom_url for collection.")
          :handler handle_update-custom-urls
          :middleware [sd/ring-wrap-add-media-resource
                       sd/ring-wrap-authorization-edit-metadata]
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:collection_id s/Str}
                       :body schema_update_custom_url}
          :responses {200 {:body schema_export_custom_url}
                      406 {:body s/Any}}}

    :delete {:summary (sd/sum_todo "Delete custom_url for collection.")
             :handler handle_delete-custom-urls
             :middleware [sd/ring-wrap-add-media-resource
                          sd/ring-wrap-authorization-edit-metadata]
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:collection_id s/Str}}
             :responses {200 {:body schema_export_custom_url}
                         404 {:body s/Any}}}}])