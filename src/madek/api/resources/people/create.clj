(ns madek.api.resources.people.create
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.people.common :as common]
   [madek.api.resources.people.common :refer [find-person-by-uid]]
   [madek.api.resources.people.get :as get-person]

   [madek.api.resources.shared :as sd]

   [madek.api.utils.auth :refer [wrap-authorize-admin!]]

   [madek.api.utils.helper :refer [array-to-map t convert-map-if-exist map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn handle-create-person
  [{{data :body} :parameters ds :tx :as req}]
  (try
    (let [{id :id} (-> (sql/insert-into :people)
                       (sql/values [(convert-map-if-exist data)])
                       sql-format
                       ((partial jdbc/execute-one! ds) {:return-keys true}))]
      (sd/response_ok (spy (find-person-by-uid id ds)) 201))
    (catch Exception e
      (error "handle-create-person failed" {:request req})
      (sd/parsed_response_exception e))))

(comment
  (let [key "string8"

        data {:institution key
              :institutional_id key

              :subtype "PeopleInstitutionalGroup"
              :external_uris ["string"]
              :pseudonym "string"
              :admin_comment "string"
              :last_name "string"
              :first_name "string"
              :description "string"}

        query (-> (sql/insert-into :people)
                  (sql/values [(convert-map-if-exist data)])

                  ;; broken
                  ;(apply (sql/returning) [:people.created_at
                  ;(into (sql/returning) [:people.created_at
                  ;                :people.description
                  ;                :people.external_uris
                  ;                :people.id
                  ;                :people.first_name
                  ;                :people.institution
                  ;                :people.institutional_id
                  ;                :people.last_name
                  ;                :people.admin_comment
                  ;                :people.pseudonym
                  ;                :people.subtype
                  ;                :people.updated_at])

                  ;; works
                  (sql/returning :people.created_at
                                 :people.description
                                 :people.external_uris
                                 :people.id
                                 :people.first_name
                                 :people.institution
                                 :people.institutional_id
                                 :people.last_name
                                 :people.admin_comment
                                 :people.pseudonym
                                 :people.subtype
                                 :people.updated_at)

                  sql-format)
        result (jdbc/execute-one! (get-ds) query)]

    result))

(def schema
  {;; TODO: fixme, create customized schema to validate enums
   ;:subtype (s/enum "Person" "PeopleGroup" "PeopleInstitutionalGroup")
   :subtype (s/maybe s/Str)

   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :external_uris) [s/Str]
   (s/optional-key :first_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :last_name) (s/maybe s/Str)
   (s/optional-key :admin_comment) (s/maybe s/Str)
   (s/optional-key :pseudonym) (s/maybe s/Str)})

(def route
  {:accept "application/json"
   :coercion reitit.coercion.schema/coercion
   :content-type "application/json"
   :description "Create a person.\nThe [subtype] has to be one of [\"Person\" \"PeopleGroup\" \"PeopleInstitutionalGroup\"]. \nAt least one of [first_name, last_name, description] must have a value."
   :handler handle-create-person
   :middleware [wrap-authorize-admin!]
   :parameters {:body schema}
   :responses {201 {:body get-person/schema}

               409 {:description "Conflict."
                    :schema s/Str
                    :examples {"application/json" {:message "Violation of constraint"}}}}

   :summary (t "Create a person")
   :swagger {:produces "application/json"
             :consumes "application/json"}})

;### Debug ####################################################################
;(debug/debug-ns *ns*)
