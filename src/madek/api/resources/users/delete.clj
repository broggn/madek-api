(ns madek.api.resources.users.delete
  (:require
   [clj-uuid :as uuid]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.authorization :as authorization]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :refer [wrap-find-user]]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]
   [madek.api.utils.logging :as logging]
   [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]

   [next.jdbc :as jdbc]

   [reitit.coercion.schema]

   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

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
  {:summary (sd/sum_adm (t "Delete user by id"))
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
