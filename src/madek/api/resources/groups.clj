(ns madek.api.resources.groups
  (:require [clj-uuid]
            [clojure.tools.logging :as logging]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [madek.api.db.core :refer [get-ds]]
            [madek.api.pagination :as pagination]
            [madek.api.resources.groups.shared :as groups]
            [madek.api.resources.groups.users :as group-users]
            [madek.api.resources.shared :as sd]


            [madek.api.utils.helper :refer [mslurp]]
            [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]

            [madek.api.utils.auth :refer [wrap-authorize-admin!]]
            [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]
            [next.jdbc :as jdbc]
            [reitit.coercion.schema]

            [taoensso.timbre :refer [spy error info warn]]

            [schema.core :as s]))

;### create group #############################################################

;; FIXME: not in use?
(defn create-group [request]
  (let [params (as-> (:body request) params
                 (or params {})
                 (assoc params :id (or (:id params) (clj-uuid/v4))))

        p (println ">o> params" params)]

    {:body (dissoc
             (->> (jdbc/execute-one! (get-ds) (-> (sql/insert-into :groups)
                                                  (sql/values [params])
                                                  (sql/returning :*)
                                                  sql-format)))
             :previous_id :searchable)
     :status 201}))

;### get group ################################################################

(defn get-group [id-or-institutional-group-id]
  (if-let [group (groups/find-group id-or-institutional-group-id)]
    {:body (dissoc group :previous_id :searchable)}
    {:status 404 :body "No such group found"}))             ; TODO: toAsk 204 No Content

;### delete group ##############################################################

(defn delete-group [id]
  (let [
        sec (groups/jdbc-update-group-id-where-clause id)
        fir (-> (sql/delete-from :groups)
                (sql/where (:where sec))
                sql-format)
        res (jdbc/execute-one! (get-ds) fir)
        p (println ">o> res=" res)
        update-count (get res :next.jdbc/update-count)
        p (println ">o> update-count=" update-count)]

    (if (= 1 update-count)
      {:status 204 :content-type "application/json"}        ;TODO / FIXME: repsonse is of type octet-stream
      {:status 404})))

;### patch group ##############################################################
(defn db_update-group [group-id body]
  (println ">o> abc" group-id)
  (println ">o> abc2" body)


  (let [sett (-> body convert-sequential-values-to-sql-arrays)
        p (println ">o> sett" sett)

        where-clause (:where (groups/jdbc-update-group-id-where-clause group-id))
        p (println ">o> where-clause" where-clause)

        fir (-> (sql/update :groups)
                (sql/set (-> body convert-sequential-values-to-sql-arrays))
                (sql/where where-clause)
                (sql/returning :*)
                sql-format)
        p (println ">o> sql-fir" fir)

        result (jdbc/execute-one! (get-ds) fir)
        p (println ">o> res=" result)
        ]
    result)

  )

(defn patch-group [{body :body {group-id :group-id} :params}]
  (try
    (if-let [result (db_update-group group-id body)]
      {:body result}
      {:status 404})

    (catch Exception e
      (error "handle-patch-group failed, group-id=" group-id)
      (sd/parsed_response_exception e)))
  )

;### index ####################################################################
; TODO test query and paging
(defn build-index-query [req]
  (let [query-params (-> req :parameters :query)

        p (println ">o> query-params" query-params)]
    (-> (if (true? (:full_data query-params))
          (sql/select :*)
          (sql/select :id))
        (sql/from :groups)
        (sql/order-by [:id :asc])
        (sd/build-query-param query-params :id)
        (sd/build-query-param query-params :institutional_id)
        (sd/build-query-param query-params :type)

        ;(sd/build-query-param query-params :person_id) ;; TODO: FIX: person_id is not in the groups table
        (sd/build-query-param query-params :created_by_user_id)

        (sd/build-query-param-like query-params :name)
        (sd/build-query-param-like query-params :institutional_name)
        (sd/build-query-param-like query-params :institution)
        (sd/build-query-param-like query-params :searchable)
        (pagination/add-offset-for-honeysql query-params)
        sql-format
        spy
        )))

(defn index [request]
  (let [result (jdbc/execute! (get-ds) (build-index-query request))]
    ;(let [result (jdbco/query (rdbms/get-ds) (build-index-query request))]
    (sd/response_ok {:groups result})))

;### routes ###################################################################

(def schema_import-group
  {
   ;(s/optional-key :id) s/Str
   :name s/Str
   ;:type (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :type) (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)

   ;(s/optional-key :person_id) (s/maybe s/Uuid) ;; TODO: FIX: person_id is not in the groups table
   (s/optional-key :created_by_user_id) (s/maybe s/Uuid)
   })

(def schema_update-group
  {(s/optional-key :name) s/Str
   (s/optional-key :type) (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)

   ;(s/optional-key :person_id) (s/maybe s/Uuid) ;; TODO: FIX: person_id is not in the groups table
   (s/optional-key :created_by_user_id) (s/maybe s/Uuid)
   })

(def schema_export-group
  {:id s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :type) s/Str                             ; TODO enum
   (s/optional-key :created_by_user_id) (s/maybe s/Uuid)
   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)

   ;(s/optional-key :person_id) (s/maybe s/Uuid) ;; TODO: FIX: person_id is not in the groups table
   ;(s/optional-key :created_by_user_id) (s/maybe s/Uuid)

   (s/optional-key :searchable) s/Str})

(defn handle_create-group
  "TODO  catch errors"
  [request]

  (try
    (let [params (get-in request [:parameters :body])
          data_wid (assoc params :id (or (:id params) (clj-uuid/v4)))
          data_wtype (assoc data_wid :type (or (:type data_wid) "Group"))

          ;resultdb (->> (jdbco/insert! (rdbms/get-ds) :groups data_wtype) first)

          resultdb (->> (jdbc/execute-one! (get-ds) (-> (sql/insert-into :groups)
                                                        (sql/values [data_wtype])
                                                        (sql/returning :*)
                                                        sql-format)))

          result (dissoc resultdb :previous_id :searchable)]
      (logging/info (apply str ["handler_create-group: \ndata:" data_wtype "\nresult-db: " resultdb "\nresult: " result]))
      ;{:status 201 :body {:id result}}
      {:status 201 :body result})

    (catch Exception e
      (error "handle-create-group failed" {:request request})
      (sd/parsed_response_exception e)
      ))
  )

(defn handle_get-group [req]
  (let [id (-> req :parameters :path :id)]
    (logging/info "handle_get-group" "\nid\n" id)
    (get-group id)))

(defn handle_delete-group [req]
  (let [id (-> req :parameters :path :id)]
    (delete-group id)))

(defn handle_update-group [req]
  (let [id (-> req :parameters :path :id)
        body (-> req :parameters :body)]
    ;(logging/info "handle_update-group" "\nid\n" id "\nbody\n" body)
    (patch-group {:params {:group-id id} :body body})))

(def schema_query-groups
  {(s/optional-key :id) s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :type) s/Str
   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any
   (s/optional-key :institutional_id) s/Str
   (s/optional-key :institutional_name) s/Str
   (s/optional-key :institution) s/Str

   ;(s/optional-key :person_id) s/Uuid ;; TODO: FIX: person_id is not in the groups table
   (s/optional-key :created_by_user_id) s/Uuid

   (s/optional-key :searchable) s/Str
   (s/optional-key :full_data) s/Bool
   (s/optional-key :page) s/Int
   (s/optional-key :count) s/Int})

(def user-routes
  [["/groups"
    {:swagger {:tags ["groups"]                             ;;:security [{"auth" []}]
               }}

    ["/" {:get {:summary (t "Get all group ids")
                :description "Get list of group ids. Paging is used as you get a limit of 100 entries."
                :handler index
                :middleware [wrap-authorize-admin!]
                :swagger {:produces "application/json"}
                :content-type "application/json"
                :parameters {:query schema_query-groups}
                ;:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :responses {200 {:body {:groups [schema_export-group]}}}}}]
    ["/:id" {:get {:summary (t "Get group by id")
                   :description "Get group by id. Returns 404, if no such group exists."
                   :swagger {:produces "application/json"}
                   :content-type "application/json"
                   :handler handle_get-group
                   :middleware [wrap-authorize-admin!]
                   :coercion reitit.coercion.schema/coercion
                   :parameters {:path {:id s/Uuid}}
                   :responses {200 {:body schema_export-group}
                               404 {:body s/Any}}}}]]])

;; api/admin/..
(def ring-routes
  ["/groups"
   {:swagger {:tags ["admin/groups"] :security [{"auth" []}]}}

   ["/" {:get {:summary (f (t "Get all group ids") "no-input-validation")
               :description "Get list of group ids. Paging is used as you get a limit of 100 entries."
               :handler index
               :middleware [wrap-authorize-admin!]
               :swagger {:produces "application/json"}
               :parameters {:query schema_query-groups}
               ;:parameters {:query s/Any}
               :content-type "application/json"
               :coercion reitit.coercion.schema/coercion
               :responses {200 {:body {:groups [schema_export-group]}}}}

         :post {:summary (f (t "Create a group") "groups::person_id-not-exists")
                :description "Create a group."
                :handler handle_create-group
                :middleware [wrap-authorize-admin!]
                :swagger {:produces "application/json" :consumes "application/json"}
                :content-type "application/json"
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :parameters {:body schema_import-group}
                :responses {201 {:body schema_export-group}

                            ;; FIXME: because of groups::person_id-not-exists
                            404 {:description "Not Found."
                                 :schema s/Str
                                 :examples {"application/json" {:message "User entry not found"}}}

                            409 {:description "Conflict."
                                 :schema s/Str
                                 :examples {"application/json" {:message "Entry already exists"}}}

                            500 {:body s/Any}}}}]

   ["/:id" {:get {:summary (t "Get group by id")
                  :description "Get group by id. Returns 404, if no such group exists."
                  :swagger {:produces "application/json"}
                  :content-type "application/json"
                  :accept "application/json"
                  :handler handle_get-group
                  :middleware [wrap-authorize-admin!]
                  :coercion reitit.coercion.schema/coercion
                  :parameters {:path {:id s/Uuid}}
                  :responses {200 {:body schema_export-group}

                              404 {:description "Not Found."
                                   :schema s/Str
                                   :examples {"application/json" {:message "No such group found"}}}

                              ;404 {:body s/Any}
                              }}

            :delete {:summary (f (t "Deletes a group by id"))
                     :description "Delete a group by id"
                     :handler handle_delete-group
                     :middleware [wrap-authorize-admin!]
                     :coercion reitit.coercion.schema/coercion
                     :parameters {:path {:id s/Uuid}}
                     :responses {403 {:body s/Any}

                                 ;; TODO: response is of type octet-stream
                                 204 {:description "No Content. The resource was deleted successfully."
                                      :schema nil
                                      :examples {"application/json" nil}
                                      }

                                 }
                     }

            :put {:summary (t "Get group by id")
                  ;:description "Get group by id. Returns 404, if no such group exists."
                  :swagger {:produces "application/json"}
                  :content-type "application/json"
                  :accept "application/json"
                  :handler handle_update-group

                  :description (mslurp "./md/admin-groups-put.md")

                  :middleware [wrap-authorize-admin!]
                  :coercion reitit.coercion.schema/coercion
                  :parameters {:path {:id s/Uuid}
                               :body schema_update-group}
                  :responses {200 {:body s/Any}             ;groups/schema_export-group}
                              404 {:body s/Any}}}}]         ; TODO error handling

   ; groups-users/ring-routes
   ["/:group-id/users/" {:get {:summary (t "Get group users by id")
                               :description "Get group users by id. (zero-based paging)"
                               :swagger {:produces "application/json"}
                               :content-type "application/json"

                               :handler group-users/handle_get-group-users
                               :middleware [wrap-authorize-admin!]
                               :coercion reitit.coercion.schema/coercion
                               :parameters {:path {:group-id s/Uuid}
                                            :query {(s/optional-key :page) s/Int
                                                    (s/optional-key :count) s/Int}}
                               :responses {
                                           200 {:description "OK - Returns a list of group users OR an empty list."
                                                :schema {:body {:users [group-users/schema_export-group-user-simple]}}}
                               }}

                         ; TODO works with tests, but not with the swagger ui
                         ; TODO: broken test / duplicate key issue
                         :put {:summary (f (t "Update group users by group-id and list of users.") "tests-needed / BROKEN-TEST")
                               :description "Update group users by group-id and list of users."
                               :swagger {:consumes "application/json" :produces "application/json"}
                               :content-type "application/json"
                               :accept "application/json"
                               :handler group-users/handle_update-group-users
                               :coercion reitit.coercion.schema/coercion
                               :parameters {:path {:group-id s/Uuid}
                                            :body group-users/schema_update-group-user-list}

                               ;:body {:users [s/Any]}}
                               :responses {
                                           200 {:body s/Any} ;groups/schema_export-group}
                                           ;200 {:body groups/schema_export-group} ;groups/schema_export-group}
                                           404 {:body s/Str}}}}]

   ["/:group-id/users/:user-id" {:get {:summary (t "Get group user by group-id and user-id")
                                       :description "Get group user by group-id and user-id. gid: 4059e7eb-cf2d-4434-b14e-9a8b4119cfbe uid: 74feaf67-6706-469a-92a5-eff9aef9f897 "
                                       :swagger {:produces "application/json"}
                                       :content-type "application/json"
                                       :handler group-users/handle_get-group-user
                                       :middleware [wrap-authorize-admin!]
                                       :coercion reitit.coercion.schema/coercion
                                       :parameters {:path {:group-id s/Uuid :user-id s/Uuid}}
                                       :responses {200 {:body group-users/schema_export-group-user-simple}

                                                   404 {:description "Creation failed."
                                                        :schema s/Str
                                                        :examples {"application/json" {:message "No such group or user."}}}
                                                   }

                                       } ; TODO error handling

                                 :put {:summary (t "Get group user by group-id and user-id")
                                       :description "Get group user by group-id and user-id."
                                       :swagger {:produces "application/json"}
                                       :content-type "application/json"
                                       :handler group-users/handle_add-group-user
                                       :middleware [wrap-authorize-admin!]
                                       :coercion reitit.coercion.schema/coercion
                                       :parameters {:path {:group-id s/Uuid :user-id s/Uuid}}
                                       :responses {200 {:body {:users [group-users/schema_export-group-user-simple]}}

                                                   ;404 {:body s/Any}

                                                   404 {:description "Creation failed."
                                                        :schema s/Str
                                                        :examples {"application/json" {:message "No such group or user."}}}

                                                   }} ; TODO error handling

                                 :delete {:summary (t "Deletes a group-user by group-id and user-id")
                                          :description "Delete a group-user by group-id and user-id."
                                          ;:swagger {:produces "application/json"}
                                          ;:content-type "application/json"
                                          :handler group-users/handle_delete-group-user
                                          :middleware [wrap-authorize-admin!]
                                          :coercion reitit.coercion.schema/coercion
                                          :parameters {:path {:group-id s/Uuid :user-id s/Uuid}}
                                          :responses {200 {:body {:users [group-users/schema_export-group-user-simple]}}

                                                      404 {:description "Not Found."
                                                           :schema s/Str
                                                           :examples {"application/json" {:message "No such group or user."}}}


                                                      406 {:body s/Str}}}}] ; TODO error handling
   ])
;### Debug ####################################################################
;(debug/debug-ns *ns*)
