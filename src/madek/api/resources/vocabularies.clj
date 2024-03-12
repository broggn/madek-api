(ns madek.api.resources.vocabularies
  (:require

   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   [honey.sql :refer [format] :rename {format sql-format}]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.resources.shared :as sd]
   [madek.api.resources.vocabularies.index :refer [get-index]]
   [madek.api.resources.vocabularies.permissions :as permissions]

   ;; all needed imports
   [madek.api.resources.vocabularies.vocabulary :refer [get-vocabulary]]

   [madek.api.utils.auth :refer [wrap-authorize-admin!]]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]
   [next.jdbc :as jdbc]
   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist f replace-java-hashmaps t v]]


   [reitit.coercion.schema]

   [schema.core :as s]))

; TODO logwrite

;; TODO: move to shared/helpers
(defn transform_ml [data]
  (assoc data
         :labels (sd/transform_ml (:labels data))
         :descriptions (sd/transform_ml (:descriptions data))))

(defn handle_create-vocab [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)

            ;ins-res (jdbc/insert! (rdbms/get-ds) :vocabularies data)]

            sql-query (-> (sql/insert-into :vocabularies)
                          ;(sql/values [data])

                          (sql/values [(convert-map-if-exist (cast-to-hstore data))])
                          (sql/returning :*)

                          sql-format)
            ins-res (jdbc/execute-one! (get-ds) sql-query)]

        ;(if-let [result (::jdbc/update-count ins-res)]
        (if ins-res
          (sd/response_ok (transform_ml ins-res))
          (sd/response_failed "Could not create vocabulary." 406))))
    (catch Exception ex (sd/response_exception ex))))

;(defn handle_update-vocab [req]
;  (try
;    (catcher/with-logging {}
;      (let [data (-> req :parameters :body)
;            id (-> req :parameters :path :id)
;            dwid (assoc data :id id)
;            old-data (sd/query-eq-find-one :vocabularies :id id)
;            ]
;
;        (if old-data
;
;          ;(if-let [upd-res (jdbc/update! (rdbms/get-ds) :vocabularies dwid ["id = ?" id])]
;
;
;          (let [
;                sql-query (-> (sql/update :vocabularies)
;                              (sql/set-fields dwid)
;                              (sql/where [:= :id id])
;                              sql-format)
;                upd-res (jdbc/execute! (get-ds) sql-query)])
;
;          (if-let [result (::jdbc/update-count upd-res)]
;
;
;            (let [new-data (sd/query-eq-find-one :vocabularies :id id)]
;              (logging/info "handle_update-vocab"
;                "\nid: " id "\nnew-data:\n" new-data)
;              (sd/response_ok (transform_ml new-data)))
;            (sd/response_failed "Could not update vocabulary." 406))
;          (sd/response_not_found "No such vocabulary."))))
;    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-vocab [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            dwid (assoc data :id id)
            old-data (sd/query-eq-find-one :vocabularies :id id)]
        (if old-data
          (let [sql-query (-> (sql/update :vocabularies)
                              (sql/set dwid)
                              (sql/where [:= :id id])
                              sql-format)
                upd-res (jdbc/execute! (get-ds) sql-query)]
            (if-let [result (::jdbc/update-count upd-res)]
              (let [new-data (sd/query-eq-find-one :vocabularies :id id)]
                (logging/info "handle_update-vocab"
                  "\nid: " id "\nnew-data:\n" new-data)
                (sd/response_ok (transform_ml new-data)))
              (sd/response_failed "Could not update vocabulary." 406)))
          (sd/response_not_found "No such vocabulary."))))
    (catch Exception ex (sd/response_exception ex))))

;(defn handle_delete-vocab [req]
;  (try
;    (catcher/with-logging {}
;      (let [id (-> req :parameters :path :id)]
;        (if-let [old-data (sd/query-eq-find-one :vocabularies :id id)]
;
;          ;(let [db-result (jdbc/delete! (rdbms/get-ds) :vocabularies ["id = ?" id])]
;
;          (let [sql-query (-> (sql/delete-from :vocabularies)
;                              (sql/where [:= :id id])
;                              sql-format)
;                db-result (jdbc/execute-one! (get-ds) sql-query)]
;            ;; rest of your code
;            (if (= 1 (::jdbc/update-count db-result))
;              (sd/response_ok (transform_ml old-data))
;              (sd/response_failed "Could not delete vocabulary." 406)))
;          )
;
;
;        (sd/response_not_found "No such vocabulary."))))
;  (catch Exception ex (sd/response_exception ex)))

(defn handle_delete-vocab [req]
  (try
    (catch Exception ex (sd/response_exception ex)))
  (catcher/with-logging {}
    (let [id (-> req :parameters :path :id)]
      (if-let [old-data (sd/query-eq-find-one :vocabularies :id id)]
        (let [sql-query (-> (sql/delete-from :vocabularies)
                            (sql/where [:= :id id])
                            sql-format)
              db-result (jdbc/execute-one! (get-ds) sql-query)]
          ;; rest of your code
          (if (= 1 (::jdbc/update-count db-result))
            (sd/response_ok (transform_ml old-data))
            (sd/response_failed "Could not delete vocabulary." 406)))
        (sd/response_not_found "No such vocabulary.")))))

(def schema_export-vocabulary
  {:id s/Str
   :enabled_for_public_view s/Bool
   :enabled_for_public_use s/Bool
   :position s/Int
   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)

   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_import-vocabulary
  {:id s/Str
   :enabled_for_public_view s/Bool
   :enabled_for_public_use s/Bool
   :position s/Int
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_update-vocabulary
  {;(s/optional-key :enabled_for_public_view) s/Bool
   ;(s/optional-key :enabled_for_public_use) s/Bool
   (s/optional-key :position) s/Int
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_perms-update
  {(s/optional-key :enabled_for_public_view) s/Bool
   (s/optional-key :enabled_for_public_use) s/Bool})

(def schema_perms-update-user-or-group
  {(s/optional-key :use) s/Bool
   (s/optional-key :view) s/Bool})

(def schema_export-user-perms
  {:id s/Uuid
   :user_id s/Uuid
   :vocabulary_id s/Str
   :use s/Bool
   :view s/Bool})

(def schema_export-group-perms
  {:id s/Uuid
   :group_id s/Uuid
   :vocabulary_id s/Str
   :use s/Bool
   :view s/Bool})

(def schema_export-perms_all
  {:vocabulary {:id s/Str
                :enabled_for_public_view s/Bool
                :enabled_for_public_use s/Bool}

   :users [schema_export-user-perms]
   :groups [schema_export-group-perms]})


;; TODO: move to shared
(defn generate-swagger-pagination-params []
  {:produces "application/json"
   :parameters [{:name "page"
                 :in "query"
                 :description "Page number, defaults to 1"
                 :required true
                 :value 1
                 :default 1
                 :type "number"
                 :pattern "^[1-9][0-9]*$"}
                {:name "count"
                 :in "query"
                 :description "Number of items per page, defaults to 100"
                 :required true
                 :value 100
                 :default 100
                 :type "number"
                 :pattern "^[1-9][0-9]*$"}]})


; TODO vocab permission
(def admin-routes
  ["/vocabularies"
   {:swagger {:tags ["admin/vocabularies"] :security [{"auth" []}]}}
   ["/"
    {:get {:summary (t "Get list of vocabularies ids.")
           :description "Get list of vocabularies ids."
           :handler get-index
           :middleware [wrap-authorize-admin!]
           :content-type "application/json"
           :swagger (generate-swagger-pagination-params)
           :coercion reitit.coercion.schema/coercion
           :responses {200 {:body {:vocabularies [schema_export-vocabulary]}}}
           }

     :post {:summary (sd/sum_adm (t "Create vocabulary."))
            :handler handle_create-vocab
            :middleware [wrap-authorize-admin!]

            :description (slurp "./md/vocabularies-post.md")

            :content-type "application/json"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema_import-vocabulary}
            :responses {200 {:body schema_export-vocabulary}

                        406 {:description "Creation failed."
                             :schema s/Str
                             :examples {"application/json" {:message "Could not create vocabulary."}}}

                        500 {:description "Duplicate key"
                             :schema s/Str
                             :examples {"application/json" {:message "ERROR: duplicate key value violates unique constraint 'vocabularies_pkey' Detail: Key (id)=(toni_dokumentation2) already exists."}}}
                        }
            :swagger {:consumes "application/json" :produces "application/json"}}}]

   ["/:id"
    {:get {:summary (sd/sum_adm "Get vocabulary by id.")
           :description "Get a vocabulary by id. Returns 404, if no such vocabulary exists."
           :handler get-vocabulary
           :middleware [wrap-authorize-admin!]
           :swagger {:produces "application/json"}
           :content-type "application/json"

           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Str}}
           :responses {200 {:body schema_export-vocabulary}
                       404 {:body s/Any}}}

     :put {:summary (sd/sum_adm_todo "Update vocabulary.")
           :handler handle_update-vocab
           :middleware [wrap-authorize-admin!]
           :content-type "application/json"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Str}
                        :body schema_update-vocabulary}
           :responses {200 {:body schema_export-vocabulary}
                       404 {:body s/Any}}
           :swagger {:consumes "application/json" :produces "application/json"}}

     :delete {:summary (sd/sum_adm_todo "Delete vocabulary.")
              :handler handle_delete-vocab
              :middleware [wrap-authorize-admin!]
              :content-type "application/json"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:id s/Str}}
              :responses {200 {:body schema_export-vocabulary}
                          404 {:body s/Any}}
              :swagger {:produces "application/json"}}}]

   ["/:id/perms"
    ;{:swagger {:tags ["admin/vocabulary/perms"] :security [{"auth" []}]}}
    ["/"
     {:get
      {:summary (sd/sum_adm "List vocabulary permissions")
       :handler permissions/handle_list-vocab-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str}}
       :responses {200 {:body schema_export-perms_all}
                   404 {:body s/Any}}}
      :put
      {:summary (sd/sum_adm "Update vocabulary resource permissions")
       :handler handle_update-vocab
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str}
                    :body schema_perms-update}
       :responses {200 {:body schema_export-vocabulary}
                   404 {:body s/Any}}}}]

    ["/users"
     {:get
      {:summary (sd/sum_adm_todo "List vocabulary user permissions")
       :handler permissions/handle_list-vocab-user-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str}}
       :responses {200 {:body [schema_export-user-perms]}
                   404 {:body s/Any}}}}]

    ["/user/:user_id"
     {:get
      {:summary (sd/sum_adm_todo "Get vocabulary user permissions")
       :handler permissions/handle_get-vocab-user-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :user_id s/Uuid}}
       :responses {200 {:body schema_export-user-perms}
                   404 {:body s/Any}}}
      :post
      {:summary (sd/sum_adm "Create vocabulary user permissions")
       :handler permissions/handle_create-vocab-user-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :user_id s/Uuid}
                    :body schema_perms-update-user-or-group}
       :responses {200 {:body schema_export-user-perms}
                   404 {:body s/Any}}}

      :put
      {:summary (sd/sum_adm "Update vocabulary user permissions")
       :handler permissions/handle_update-vocab-user-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :user_id s/Uuid}
                    :body schema_perms-update-user-or-group}
       :responses {200 {:body schema_export-user-perms}
                   404 {:body s/Any}}}

      :delete
      {:summary (sd/sum_adm "Delete vocabulary user permissions")
       :handler permissions/handle_delete-vocab-user-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :user_id s/Uuid}}
       :responses {200 {:body schema_export-user-perms}
                   404 {:body s/Any}}}}]

    ["/groups"
     {:get
      {:summary (sd/sum_adm_todo "List vocabulary group permissions")
       :handler permissions/handle_list-vocab-group-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str}}
       :responses {200 {:body [schema_export-group-perms]}
                   404 {:body s/Any}}}}]

    ["/group/:group_id"
     {:get
      {:summary (sd/sum_adm_todo "Get vocabulary group permissions")
       :handler permissions/handle_get-vocab-group-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :group_id s/Uuid}}
       :responses {200 {:body schema_export-group-perms}
                   404 {:body s/Any}}}

      :post
      {:summary (sd/sum_adm_todo "Create vocabulary group permissions")
       :handler permissions/handle_create-vocab-group-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :group_id s/Uuid}
                    :body schema_perms-update-user-or-group}
       :responses {200 {:body schema_export-group-perms}
                   404 {:body s/Any}}}

      :put
      {:summary (sd/sum_adm_todo "Update vocabulary group permissions")
       :handler permissions/handle_update-vocab-group-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :group_id s/Uuid}
                    :body schema_perms-update-user-or-group}
       :responses {200 {:body schema_export-group-perms}
                   404 {:body s/Any}}}

      :delete
      {:summary (sd/sum_adm_todo "Delete vocabulary group permissions")
       :handler permissions/handle_delete-vocab-group-perms
       :middleware [wrap-authorize-admin!]
       :content-type "application/json"
       :accept "application/json"
       :coercion reitit.coercion.schema/coercion
       :parameters {:path {:id s/Str
                           :group_id s/Uuid}}
       :responses {200 {:body schema_export-group-perms}
                   404 {:body s/Any}}}}]]])

(def user-routes
  ["/vocabularies"
   {:swagger {:tags ["admin/people"]}}

   ["/" {:get {:summary "Get list of vocabularies ids."
               :description "Get list of vocabularies ids."
               :handler get-index
               :content-type "application/json"
               :coercion reitit.coercion.schema/coercion
               :parameters {:query {(s/optional-key :page) s/Int}}
               :responses {200 {:body {:vocabularies [schema_export-vocabulary]}}}
               :swagger {:produces "application/json"}}}]

   ["/:id" {:get {:summary "Get vocabulary by id."
                  :description "Get a vocabulary by id. Returns 404, if no such vocabulary exists."
                  :swagger {:produces "application/json"}
                  :content-type "application/json"
                  :handler get-vocabulary
                  :coercion reitit.coercion.schema/coercion
                  :parameters {:path {:id s/Str}}
                  :responses {200 {:body schema_export-vocabulary}
                              404 {:body s/Any}}}}]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
