(ns madek.api.resources.users.delete
  (:require
   [clj-uuid :as uuid]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :refer [wrap-find-user]]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [t]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]))

(defn delete-user
  "Delete a user by its id and returns true if delete was succesfull
  and false otherwise."
  [id tx]
  (-> (sql/delete-from :users)
      (sql/where [:= :users.id (uuid/as-uuid id)])
      (sql-format :inline false)
      (->> (jdbc/execute-one! tx))
      :next.jdbc/update-count
      (= 1)))

(defn handler
  [{{id :id :as user} :user ds :tx :as req}]
  (try
    (if (delete-user id ds)
      (sd/response_ok user)
      (sd/response_failed "Could not delete user." 406))
    (catch Exception ex (sd/parsed_response_exception ex))))

(def route
  {:summary (sd/sum_adm "Delete user by id")
   :description "Delete a user by id. Returns 404, if no such user exists."
   :handler handler
   :middleware [wrap-authorize-admin!
                (wrap-find-user :id)]
   :swagger {:produces "application/json"}
   :coercion reitit.coercion.schema/coercion
   :content-type "application/json"
   :parameters {:path {:id s/Str}}
   :responses {200 {:body get-user/schema}
               403 {:description "Forbidden."
                    :schema s/Str
                    :examples {"application/json" {:message "References still exist"}}}
               404 {:description "Not Found."
                    :schema s/Str
                    :examples {"application/json" {:message "No such user."}}}}})
