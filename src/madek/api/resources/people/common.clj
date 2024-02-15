(ns madek.api.resources.people.common
  (:require
   [clj-uuid :as uuid :refer [as-uuid]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.utils.json :as json]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

;;; schema ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; sql ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def people-select-keys
  [:people.created_at
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
   :people.updated_at])

(defn where-uid
  "Adds a where condition to the people people query against a unique id. The
  uid can be either the id, or the json encoded pair [insitution, institutional_id]."
  ([sql-map uid]
   (-> sql-map
       (sql/where
        (if (uuid/uuidable? uid)
          [:= :id (as-uuid uid)]
          (let [[institution institutional_id] (json/decode uid)]
            [:and
             [:= :people.institution institution]
             [:= :people.institutional_id institutional_id]]))))))

(def base-query
  (-> (apply sql/select people-select-keys)
      (sql/from :people)))

(defn person-query [uid]
  (-> base-query
      (where-uid uid)))

(defn find-person-by-uid [uid ds]
  (-> (person-query uid)
      sql-format
      (->> (jdbc/execute-one! ds))))
