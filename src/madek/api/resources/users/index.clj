(ns madek.api.resources.users.index
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.users.common :as common]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]
   [madek.api.utils.pagination :as pagination]

   [next.jdbc :as jdbc]

   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn handle-email-clause [thread-obj params]
  (if-let [email (:email params)]
    (-> thread-obj
        (sql/where [:= :email email])        )
    thread-obj))

(defn handler
  "Get an index of the users. Query parameters are pending to be implemented."
  [{params :params tx :tx :as req}]

  (let [ query (-> common/base-query
                  (pagination/sql-offset-and-limit params)
                  (handle-email-clause params)
                  (sql-format :inline false))
        res (->> query
                 (jdbc/execute! tx)
                 (assoc {} :users))
        res (sd/transform_ml_map res)        ]


    (sd/response_ok res)))

(def query-schema
  {(s/optional-key :count) s/Int
   (s/optional-key :email) s/Str
   (s/optional-key :page) s/Int})

(def route
  {:summary (sd/sum_adm (f (t "Get list of users ids.") "no-list"))
   :description "Get list of users ids."
   :swagger {:produces "application/json"
             :parameters [{:name "email"
                           :in "query"
                           :description "Filter admin by email, e.g.: mr-test@zhdk.ch"
                           :type "string"
                           :pattern "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"}
                          {:name "page"
                           :in "query"
                           :description "Page number, defaults to 0 (zero-based-index)"
                           :required true
                           :default 0
                           :minimum 0
                           :type "number"
                           ;:type "integer"
                           ;:type "long"
                           ;:pattern "^([1-9][0-9]*|0)$"
                           }
                          {:name "count"
                           :in "query"
                           :description "Number of items per page (1-100), defaults to 100"
                           :required true
                           :minimum 1
                           :maximum 100
                           :value 100
                           :default 100
                           ;:type "integer"
                           :type "number"
                           ;:type "long"
                           }]}
   :content-type "application/json"
   :handler handler
   :middleware [wrap-authorize-admin!]
   :coercion reitit.coercion.schema/coercion
   :responses {200 {:body {:users [get-user/schema]}}}})
