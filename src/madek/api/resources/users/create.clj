(ns madek.api.resources.users.create
  (:require
   [clj-uuid :as uuid]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.authorization :as authorization]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]

   [clojure.data.json :as json]

   [madek.api.utils.validation :refer [email-validation json-and-json-str-validation json-and-json-str-validation vector-or-hashmap-validation]]


   [madek.api.resources.users.common :refer [find-user-by-uid]]
   [madek.api.resources.users.get :as get-user]
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.logging :as logging]
   [madek.api.utils.sql-next :refer [convert-sequential-values-to-sql-arrays]]
   [madek.api.utils.helper :refer [convert-map-if-exist]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]

   ;[madek.api.resources.users.get :refer [schema]]
   [madek.api.resources.users.get :as users-get]

   [schema.core :as s]

   ;[schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

;#### create ##################################################################

(defn handle-create-user
  [{{data :body} :parameters ds :tx :as req}]
  (try

    ;(s/validate users-get/schema data)


    (let [

          p (println ">o> data" data)
          p (println ">o> data.sett" (:settings data))
          p (println ">o> data.sett.cl" (class (:settings data)))

          ;; TODO:
          ;; allowed: [] OR {}
          ;; previous: issue with settings ("{}" vs {} vs [] vs "[]") / normalization?
          data (convert-map-if-exist data)

          p (println ">o> data" data)
          p (println ">o> data.sett" (:settings data))
          p (println ">o> data.sett.cl" (class (:settings data)))

          ;{id :id} (-> (sql/insert-into :users)
          ;             (sql/values [data])
          ;             sql-format
          ;             ((partial jdbc/execute-one! ds) {:return-keys true}))

          query (-> (sql/insert-into :users)
                    (sql/values [data])
                    (sql/returning :*)
                    sql-format
                    ;((partial jdbc/execute-one! ds) {:return-keys true})
                    )

          result (jdbc/execute-one! ds query)
          ;result nil
          ;result []

          p (println ">o> result" result)

          ]

      (if result
        (do
          (println ">o> result-ok")
          (sd/response_ok result 201))                      ;; looses content type
        ;(sd/response_ok "alles suppi"))
        ;(sd/response_ok {:test "alles suppi"}))
        ;(sd/response_ok {} 201))
        (do
          (println ">o> failed")
          (sd/response_failed)))

      )

    (catch Exception e
      (error "handle-create-user failed" {:request req})
      (sd/parsed_response_exception e))))


(def schema
  {:person_id s/Uuid
   (s/optional-key :accepted_usage_terms_id) (s/maybe s/Uuid)
   (s/optional-key :email) email-validation
   (s/optional-key :first_name) s/Str
   (s/optional-key :institution) s/Str
   (s/optional-key :institutional_id) s/Str
   (s/optional-key :last_name) s/Str
   (s/optional-key :login) s/Str
   (s/optional-key :notes) (s/maybe s/Str)

   ;(s/optional-key :settings) json-and-json-str-validation
   (s/optional-key :settings) vector-or-hashmap-validation

   })


;; post /users
(def route
  {:accept "application/json"
   :coercion reitit.coercion.schema/coercion
   :content-type "application/json"
   ;:description "Create user."

   :description (slurp "./md/admin-users-post.md")

   :handler handle-create-user
   :middleware [wrap-authorize-admin!]
   :parameters {:body schema}
   :responses {
               201 {:description "Created."
                    :body s/Any
                    ;:body get-user/schema
                    }

               400 {:description "Bad Request"
                    :body s/Any
                    }

               404 {:description "Not Found."
                    :schema s/Str
                    :examples {"application/json" {:message "People entry not found"}}}

               409 {:description "Conflict."
                    :schema s/Str
                    :examples {"application/json" {:message "Entry already exists"}}}
               }
   :summary (sd/sum_adm "Create user.")
   :swagger {:consumes "application/json"
             :produces "application/json"}})

;### Debug ####################################################################
;(debug/debug-ns *ns*)
