(ns madek.api.resources.users.index
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [logbug.debug :as debug]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :as common]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.pagination :as pagination]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn handler
  "Get an index of the users. Query parameters are pending to be implemented."
  [{{query :query} :parameters tx :tx :as req}]
  (-> common/base-query
      (pagination/sql-offset-and-limit query)
      (sql-format :inline false)
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
