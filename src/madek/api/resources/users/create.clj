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

   [schema.core  :as s]

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

          data (convert-map-if-exist data)

          p (println ">o> data" data)
          p (println ">o> data.sett" (:settings data))
          p (println ">o> data.sett.cl" (class (:settings data)))

          ;{id :id} (-> (sql/insert-into :users)
          ;             (sql/values [data])
          ;             sql-format
          ;             ((partial jdbc/execute-one! ds) {:return-keys true}))

          ;query (-> (sql/insert-into :users)
          ;          (sql/values [data])
          ;          (sql/returning :*)
          ;          sql-format
          ;          ;((partial jdbc/execute-one! ds) {:return-keys true})
          ;          )
          ;
          ;result (jdbc/execute-one! ds query)
          result nil
          result []

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



;(s/defn valid-email?
;  [email]
;   (println ">o> PROCESS VALIDATION!!!!!!" email)
;
;  (re-matches #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$" email))

;(defn valid-email?
;  [email]
;   (println ">o> PROCESS VALIDATION!!!!!!" email)
;
;  (re-matches #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$" email))
;
;(defn validemail?
;  [email _]
;   (println ">o> PROCESS VALIDATION!!!!!!" email)
;
;  (re-matches #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$" email))
;
;;; Define a custom validation function for JSON
;(s/defn valid-json?
;  [json-str]
;  (try
;    (json/read-str json-str)
;    (catch Exception e
;      false)))


;(s/defn valid-email? :- s/Bool
;  [email :- s/Str]
;
;  (println "VALIDATE EMAIL!!!!!!!!!!!!!" email)
;
;  (re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$" email))
;
;
;;(def Email
;;  (and s/Str valid-email?))
;;
;;(def Email
;;  (s/with-fn-validation s/Str validemail?))
;
;
;;; Define a predicate function for email validation
;(defn validemail? [email]
;  (re-matches #".+@.+\..+" email))
;
;(def email-schema
;  (s/pred validemail? "Invalid email format"))


;(s/defn valid-email?
;  [email]
;  (re-matches #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$" email))
;
;(def email (s/pred #(re-matches #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$" %)))


(defn valid-email? [email]
  (re-matches #".+@.+\..+" email))

(def email-schema
  (s/pred valid-email? "Invalid email format"))






;; Define a regular expression for email validation
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")

;(s/defschema schema
(def schema
  {:person_id s/Uuid
   (s/optional-key :accepted_usage_terms_id) (s/maybe s/Uuid)
   ;:email (s/cond-pre s/Str (s/with-fn-validation valid-email? s/Str))
   ;(:email (s/with-fn-validation valid-email?)) s/Str

   ;(s/required-key :email) (s/with-fn-validation valid-email? s/Str)
   ;(s/required-key :email) (s/with-fn-validation valid-email? s/Str)

   ;(s/optional-key :email) (s/with-fn-validation valid-email? s/Str)
   ;(s/optional-key :email) (s/with-fn-validation valid-email? s/Str)
   ;(s/optional-key :email) (s/with-fn-validation valid-email? s/Regex)
   ;(s/optional-key :email) (s/with-fn-validation valid-email?)

   ;(s/optional-key :email) Email

   ;:email s/Str


   :email (s/constrained s/Str #(re-matches email-regex %)
           "Invalid email address")



   ;:email (s/with-fn-validation valid-email? s/Str)
   ;:email (s/with-fn-validation email s/Regex)
   ;:email (s/with-fn-validation email)

   ;(s/optional-key :email) email-schema



;:email (s/with-fn-validation validemail? s/Str)
   ;:email (s/with-fn-validation valid-email? s/Regex)
   ;:email (s/with-fn-validation valid-email? s/Str)

   ;(s/optional-key :email) email-schema


;:email (s/with-fn-validation validemail?)

   ;(s/optional-key :email) (s/Regex #"^[\w\.-]+@([\w-]+\.)+[\w-]{2,4}$")

   ;(s/optional-key :email) Email
   (s/optional-key :first_name) s/Str
   (s/optional-key :institution) s/Str
   (s/optional-key :institutional_id) s/Str
   (s/optional-key :last_name) s/Str
   (s/optional-key :login) s/Str
   (s/optional-key :notes) (s/maybe s/Str)
   (s/optional-key :settings) s/Any
   })

(defn validate-schema! [handler]
  (fn [req]
    (let [
          p (println ">o> PROCESS BODY VALIDATION")
          body (-> req :parameters :body)
          p (println ">o> 0 body=" body)

          ;query-params (-> req :parameters :query-params)
          ;p (println ">o> 0 query-params" query-params)

          ]

      (try

        (do
          (println ">o> before-val")
          ;(s/validate users-get/schema body)
          (s/validate schema body)

          p (println ">o> schema" schema)
          p (println ">o> body" body)
          p (println ">o> process validation, result: " (s/validate schema body))

          (println ">o> after-val"))


        (catch Exception ex

          (println ">o> e1" (ex-message ex))
          (println ">o> e2" (ex-cause ex))
          (println ">o> e3" (ex-data ex))

          (throw (ex-info "Invalid request body." {:status 402}))

          )
        )

      )
    )
  )

(def route
  {:accept "application/json"
   :coercion reitit.coercion.schema/coercion
   :content-type "application/json"
   ;:description "Create user."

   :description (slurp "./md/admin-users-post.md")

   :handler handle-create-user
   :middleware [
                ;validate-schema!
                wrap-authorize-admin!]
   :parameters {:body schema}
   :responses {

               201 {:description "Created."
                    :body s/Any
                    ;:body get-user/schema
                    }

               400 {:description "Bad Request"
                    :body s/Any
                    }

               409 {:description "Conflict."
                    :schema s/Str
                    :examples {"application/json" {:message "Entry already exists"}}}
               }
   :summary (sd/sum_adm "Create user.")
   :swagger {:consumes "application/json"
             :produces "application/json"}})

;### Debug ####################################################################
;(debug/debug-ns *ns*)
