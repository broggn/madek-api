(ns madek.api.resources.users.index
  (:require
   [clj-uuid :as uuid]
   [honey.sql  :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.authorization :as authorization]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :as common]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.logging :as logging]
   [madek.api.utils.pagination :as pagination]
   [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn handler
  "Get an index of the users. Query parameters are pending to be implemented."
  [{{query :query} :parameters tx :tx :as req}]
  (-> common/base-query
      (pagination/sql-offset-and-limit query)
      (sql-format :inline true)
      (->> (jdbc/execute! tx)
           (assoc {} :users))
      sd/response_ok))

(def query-schema
  {(s/optional-key :count) s/Int
   (s/optional-key :email) s/Str
   (s/optional-key :page) s/Int})

(def route
  {:summary (sd/sum_adm "Get list of users ids.")
   :description "Get list of users ids."
   :swagger {:produces "application/json"}
   :parameters {:query query-schema}
   :content-type "application/json"
   :handler handler
   :middleware [wrap-authorize-admin!]
   :coercion reitit.coercion.schema/coercion
   :responses {200 {:body {:users [get-user/schema]}}}})
