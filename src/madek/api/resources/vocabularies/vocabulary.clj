(ns madek.api.resources.vocabularies.vocabulary
  (:require
   [clojure.java.jdbc :as jdbc]
   [madek.api.resources.locales :refer [add-field-for-default-locale]]
   [madek.api.resources.shared :as sd]
   [madek.api.resources.vocabularies.permissions :as permissions]
   [madek.api.utils.rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sql]))

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
      (sql/format)))

; TODO for admin do not remove internal keys (admin_comment)
; TODO add flag for default locale
(defn get-vocabulary [request]
  (let [id (-> request :parameters :path :id)
        user-id (-> request :authenticated-entity :id)
        query (build-vocabulary-query id user-id)]
    (if-let [vocabulary (first (jdbc/query (get-ds) query))]
      ;{:body (add-fields-for-default-locale (remove-internal-keys vocabulary))}
      (sd/response_ok (transform_ml (sd/remove-internal-keys vocabulary)))
      (sd/response_failed "Vocabulary could not be found!" 404))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
