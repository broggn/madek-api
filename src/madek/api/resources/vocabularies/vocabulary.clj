(ns madek.api.resources.vocabularies.vocabulary
  (:require
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]

   [clojure.string :as str]
   ;[madek.api.utils.rdbms :refer [get-ds]]

   ;[clojure.java.jdbc :as jdbc]
   [madek.api.resources.locales :refer [add-field-for-default-locale]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.vocabularies.permissions :as permissions]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

(defn transform_ml [vocab]
  (assoc vocab
         :labels (sd/transform_ml (:labels vocab))
         :descriptions (sd/transform_ml (:descriptions vocab))))

(defn- add-fields-for-default-locale
  [result]
  (add-field-for-default-locale
    "label" (add-field-for-default-locale
              "description" result)))

(defn- where-clause
  [id user-id]
  (let [public [:= :vocabularies.enabled_for_public_view true]
        id-match [:= :vocabularies.id id]]
    (if user-id
      (let [vocabulary-ids (permissions/accessible-vocabulary-ids user-id)]
        [:and
         (if-not (empty? vocabulary-ids)
           [:or
            public
            [:in :vocabularies.id vocabulary-ids]]
           public)
         id-match])
      [:and public id-match])))

(defn build-vocabulary-query [id user-id]
  (-> (sql/select :*)
      (sql/from :vocabularies)
      (sql/where (where-clause id user-id))
      (sql-format)))

; TODO add flag for default locale
(defn get-vocabulary [request]                              ;; HERE
  (let [id (-> request :parameters :path :id)
        user-id (-> request :authenticated-entity :id)
        query (build-vocabulary-query id user-id)
        is_admin_endpoint (str/includes? (-> request :uri) "/admin/")
        db-result (jdbc/execute-one! (get-ds) query)
        p (println ">o> result" db-result)

        ;result (if (and (not (nil? db-result)) (is_admin_endpoint))
        ;         (-> db-result
        ;             transform_ml)
        ;         (-> db-result
        ;             transform_ml
        ;             sd/remove-internal-keys))

        result (if (not (nil? db-result))
                 (if is_admin_endpoint
                   (-> db-result
                       transform_ml)
                   (-> db-result
                       transform_ml
                       sd/remove-internal-keys)))


        p (println ">o> result" result)
        ]
    (if result
      (sd/response_ok result)
      (sd/response_failed "Vocabulary could not be found!" 404))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
