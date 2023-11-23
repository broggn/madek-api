(ns madek.api.resources.workflows
  (:require [cheshire.core :as cheshire]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [logbug.catcher :as catcher]
            [madek.api.authorization :as authorization]
            [madek.api.resources.shared :as sd]
            [madek.api.utils.rdbms :as rdbms :refer [get-ds]]
            [reitit.coercion.schema]
            [schema.core :as s]))

(defn handle_list-workflows
  [req]
  (let [qd (if (true? (-> req :parameters :query :full_data))
             :workflows.*
             :workflows.id)
        db-result (sd/query-find-all :workflows qd)]
    ;(logging/info "handle_list-workflows" "\nqd\n" qd "\nresult\n" db-result)
    (sd/response_ok db-result)))

(defn handle_get-workflow
  [req]
  (let [workflow (-> req :workflow)]
    (logging/info "handle_get-workflow" workflow)
    ; TODO hide some fields
    (sd/response_ok workflow)))

(defn handle_create-workflow [req]

  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            conf-data-or-str (:configuration data)
              ;conf-parsed (cheshire/parse-string conf-data)
            conf-data (sd/try-as-json conf-data-or-str)
            uid (-> req :authenticated-entity :id)
            ins-data (assoc data :creator_id uid :configuration (with-meta conf-data {:pgtype "jsonb"}))
            ins-res (jdbc/insert! (rdbms/get-ds) :workflows ins-data)]

        (logging/info "handle_create-workflow: "
                      "\ndata:\n" ins-data
                      "\nresult:\n" ins-res)

        (if-let [result (first ins-res)]
          (sd/response_ok result)
          (sd/response_failed "Could not create workflow." 406))))
    (catch Exception e (sd/response_exception e))))

(defn handle_update-workflow [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            dwid (assoc data :id id)
            upd-query (sd/sql-update-clause "id" (str id))
            upd-result (jdbc/update! (rdbms/get-ds)
                                     :workflows
                                     dwid upd-query)]

        (logging/info "handle_update-workflow: " "\nid\n" id "\ndwid\n" dwid "\nupd-result:" upd-result)

        (if (= 1 (first upd-result))
          (sd/response_ok (sd/query-eq-find-one :workflows :id id))
          (sd/response_failed "Could not update workflow." 406))))
    (catch Exception e (sd/response_exception e))))

(defn handle_delete-workflow [req]
  (try
    (catcher/with-logging {}
      (let [olddata (-> req :workflow)
            id (-> req :parameters :path :id)
            delresult (jdbc/delete! (rdbms/get-ds)
                                    :workflows
                                    ["id = ?" id])]
        (if (= 1 (first delresult))
          (sd/response_ok olddata)
          (sd/response_failed "Could not delete workflow." 422))))
    (catch Exception e (sd/response_exception e))))

(defn wwrap-find-workflow [param]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler param
                                    :workflows :id
                                    :workflow true))))

(def schema_create_workflow
  {;:id is db assigned or optional
   :name s/Str
   ;:creator_id s/Uuid
   (s/optional-key :is_active) s/Bool
   ; TODO docu is json
   (s/optional-key :configuration) s/Any})

(def schema_update_workflow
  {;:id s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :is_active) s/Bool
   ; TODO docu is json
   (s/optional-key :configuration) s/Any})

; TODO Inst coercion
(def schema_export_workflow
  {:id s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :is_active) s/Bool
   ; TODO docu is json
   (s/optional-key :configuration) s/Any
   (s/optional-key :creator_id) s/Uuid
   (s/optional-key :created_at) s/Any ; TODO as Inst
   (s/optional-key :updated_at) s/Any})

; TODO response coercion
; TODO docu
; TODO tests
(def user-routes

  ["/workflows"
   ["/"
    {:post {:summary (sd/sum_adm "Create workflow.")
            :handler handle_create-workflow
            :middleware [authorization/wrap-authorized-user]
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema_create_workflow}
            :responses {200 {:body schema_export_workflow}
                        406 {:body s/Any}}}

     :get {:summary  (sd/sum_adm "List workflows.")
           :handler handle_list-workflows
           :middleware [authorization/wrap-authorized-user]
           :coercion reitit.coercion.schema/coercion
           :parameters {:query {;(s/optional-key :name) s/Str ; TODO query by name
                                (s/optional-key :full_data) s/Bool}}
           :responses {200 {:body [schema_export_workflow]}
                       406 {:body s/Any}}}}]

   ["/:id"
    {:get {:summary (sd/sum_adm "Get workflow by id.")
           :handler handle_get-workflow
           :middleware [authorization/wrap-authorized-user
                        (wwrap-find-workflow :id)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}}
           :responses {200 {:body schema_export_workflow}
                       404 {:body s/Any}}}

     :put {:summary (sd/sum_adm "Update workflow with id.")
           :handler handle_update-workflow
           :middleware [authorization/wrap-authorized-user
                        (wwrap-find-workflow :id)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}
                        :body schema_update_workflow}
           :responses {200 {:body schema_export_workflow}
                       404 {:body s/Any}
                       406 {:body s/Any}}}

     :delete {:summary (sd/sum_adm "Delete workflow by id.")
              :coercion reitit.coercion.schema/coercion
              :handler handle_delete-workflow
              :middleware [authorization/wrap-authorized-user
                           (wwrap-find-workflow :id)]
              :parameters {:path {:id s/Uuid}}
              :responses {200 {:body schema_export_workflow}
                          404 {:body s/Any}}}}]])

