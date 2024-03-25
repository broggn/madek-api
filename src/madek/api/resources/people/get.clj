(ns madek.api.resources.people.get
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [logbug.debug :as debug]
   [madek.api.resources.people.common :refer [person-query]]
   [madek.api.resources.shared :as sd]

   [madek.api.db.core :refer [get-ds]]

   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]

   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def schema
  {:created_at s/Any
   :description (s/maybe s/Str)
   :external_uris [s/Str]
   :first_name (s/maybe s/Str)
   :id s/Uuid
   :institution s/Str
   :institutional_id (s/maybe s/Str)
   :last_name (s/maybe s/Str)
   :admin_comment (s/maybe s/Str)
   :pseudonym (s/maybe s/Str)
   :subtype (s/enum "Person" "PeopleGroup" "PeopleInstitutionalGroup")
   :updated_at s/Any})

(defn handler
  [{{{id :id} :path} :parameters
    ds :tx :as req}]

  (println ">o>people.get / handler, id=" id)
  (println ">o>people.get / handler, id.cl=" (class id))


  (debug req)
  (debug id)
  (if-let [person (-> (person-query id)
                      spy
                      sql-format
                      spy
                      (->> (jdbc/execute-one! ds)))]
    (sd/response_ok person)
    (sd/response_failed "No such person found" 404)))

(def route
  {:summary (sd/sum_adm (t "Get person by uid"))
   :description "Get a person by uid (either uuid or pair of json encoded [institution, institutional_id]). Returns 404, if no such people exists."
   :handler handler
   :middleware []
   :swagger {:produces "application/json"}
   :coercion reitit.coercion.schema/coercion
   :content-type "application/json"

   :parameters {:path {:id s/Str}}

   :responses {200 {:body schema}

               404 {:description "Not found."
                    :schema s/Str
                    :examples {"application/json" {:message "No such person found."}}}

               }})
