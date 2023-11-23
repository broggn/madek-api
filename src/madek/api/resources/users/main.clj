(ns madek.api.resources.users.main
  (:require
   [clj-uuid :as uuid]
   [honey.sql  :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.authorization :as authorization]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :as users-common
    :refer [wrap-find-user find-user-by-uid user-get-schema]]
   [madek.api.resources.users.update :as update-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.logging :as logging]
   [madek.api.utils.pagination :as pagination]
   [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

; There are some things missing here yet. A non admin user should be able to
; get limited users set (by properties and number of resutls). The index for
; admins misses useful query params.
; This is pending because of possible future changes of the relation between
; the users and the people table.

;### swagger io schema #############################################################

(def schema_create_user_result
  {:accepted_usage_terms_id (s/maybe s/Uuid) ; TODO
   :autocomplete s/Str
   :created_at s/Any
   :email (s/maybe s/Str)
   :id s/Uuid
   :institutional_id (s/maybe s/Str)
   :is_deactivated s/Bool
   :last_signed_in_at (s/maybe s/Any) ; TODO
   :login (s/maybe s/Str)
   :notes (s/maybe s/Str) ; TODO
   :person_id s/Uuid
   :searchable (s/maybe s/Str)
   :settings s/Any ; TODO is json
   :updated_at s/Any
   (s/optional-key :institution) (s/maybe s/Str)})

(def schema_create_user
  {:person_id s/Uuid
   (s/optional-key :accepted_usage_terms_id) (s/maybe s/Uuid) ; TODO
   (s/optional-key :autocomplete) s/Str
   (s/optional-key :email) s/Str
   (s/optional-key :id) s/Uuid
   (s/optional-key :institution) s/Str
   (s/optional-key :institutional_id) s/Str
   (s/optional-key :is_deactivated) s/Bool
   (s/optional-key :login) s/Str
   (s/optional-key :notes) (s/maybe s/Str) ; TODO
   (s/optional-key :searchable) s/Str
   (s/optional-key :settings) s/Any ; TODO is json
   })
(def query-users-schema
  {(s/optional-key :count) s/Int
   (s/optional-key :email) s/Str
   (s/optional-key :page) s/Int})

;##############################################################################
;#### handlers ################################################################
;##############################################################################

(defonce ^:dynamic request nil)

;#### index ###################################################################

(defn index-handler
  "Get an index of the users. Query parameters are pending to be implemented."
  [{{query :query} :parameters tx :tx :as req}]
  (debug req)
  (def ^:dynamic request req)
  (-> users-common/base-query
      (pagination/sql-offset-and-limit query)
      (sql-format :inline true)
      (->> (jdbc/execute! tx)
           (assoc {} :users))
      sd/response_ok))

;#### get user ################################################################

(defn delete-user
  "Delete a user by its id and returns true if delete was succesfull
  and false otherwise."
  [id tx]
  (-> (sql/delete-from :users)
      (sql/where [:= :users.id (uuid/as-uuid id)])
      (sql-format :inline true)
      (->> (jdbc/execute-one! tx))
      :next.jdbc/update-count
      (= 1)))

(defn delete-user-handler
  [{{id :id :as user} :user ds :tx :as req}]
  (if (delete-user id ds)
    (sd/response_ok user)
    (sd/response_failed "Could not delete user." 406)))

;#### get user ################################################################

(defn get-user-handler
  [{user :user :as req}]
  (sd/response_ok user))

;#### create ##################################################################

(defn handle-create-user
  [{{data :body} :parameters ds :tx :as req}]
  (try
    (if-let [user (-> (sql/insert-into :users)
                      (sql/values [data])
                      (sql-format :inline true)
                      ((partial jdbc/execute-one! ds) {:return-keys true}))]
      (sd/response_ok user 201)
      (sd/response_failed "Could not create user." 406))
    (catch Exception e
      (error "handle-create-user failed" {:request req})
      (sd/response_exception e))))

;### routes ###################################################################

(def admin-routes
  ["/users"
   ["/"
    {:get {:summary (sd/sum_adm "Get list of users ids.")
           :description "Get list of users ids."
           :swagger {:produces "application/json"}
           :parameters {:query query-users-schema}
           :content-type "application/json"
           :handler index-handler
           :middleware [wrap-authorize-admin!]
           :coercion reitit.coercion.schema/coercion
           :responses {200 {:body {:users [user-get-schema]}}}}

     :post {:summary (sd/sum_adm "Create user.")
            :description "Create user."
            :swagger {:consumes "application/json"
                      :produces "application/json"}
            :content-type "application/json"
            :accept "application/json"
            :handler handle-create-user
            :middleware [wrap-authorize-admin!]
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema_create_user}
            :responses {201 {:body schema_create_user_result}
                        406 {:body s/Any}}}}]

   ["/:id"
    (merge
     {:get {:summary (sd/sum_adm "Get user by id")
            :description "Get a user by id. Returns 404, if no such users exists."
            :handler get-user-handler
            :middleware [wrap-authorize-admin!
                         (wrap-find-user :id)]
            :swagger {:produces "application/json"}
            :coercion reitit.coercion.schema/coercion
            :content-type "application/json"
            :parameters {:path {:id s/Str}}

            :responses {200 {:body user-get-schema}
                        404 {:body s/Any}}}

      :delete {:summary (sd/sum_adm "Delete user by id")
               :description "Delete a user by id. Returns 404, if no such user exists."
               :handler delete-user-handler
               :middleware [wrap-authorize-admin!
                            (wrap-find-user :id)]
               :swagger {:produces "application/json"}
               :coercion reitit.coercion.schema/coercion
               :content-type "application/json"
               :parameters {:path {:id s/Str}}
               :responses {200 {:body s/Any}
                           404 {:body s/Any}}}}
     update-user/routes)]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
