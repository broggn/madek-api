(ns madek.api.resources.users.common
  (:require
   [clj-uuid :as uuid]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.resources.shared :as sd]
   [next.jdbc :as jdbc]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error info spy warn]]))

;;; schema ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; sql ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-select-keys
  [:users.accepted_usage_terms_id
   :users.created_at
   :users.email
   :users.first_name
   :users.id
   :users.institution
   :users.institutional_id
   :users.last_name
   :users.last_signed_in_at
   :users.login
   :users.notes
   :users.person_id
   :users.settings
   :users.updated_at])

(defn where-uid
  "Adds a where condition to the users users query against a unique id. The uid
  can be either the id, the email_address, or the login. If uid is a UUID only
  users.id can be a match, otherwise either email or could be a match."
  ([sql-map uid]
   (-> sql-map
       (sql/where
        (if (uuid/uuidable? uid)
          [:= :users.id (uuid/as-uuid uid)]
          [:or
           [:= :users.login [:lower uid]]
           [:= [:lower :users.email] [:lower uid]]])))))

(def is-admin-sub
  [:exists
   (-> (sql/select :true)
       (sql/from :admins)
       (sql/where [:= :users.id :admins.user_id]))])

(def base-query
  (-> (apply sql/select user-select-keys)
      (sql/select [is-admin-sub :is_admin])
      (sql/from :users)))

;;; other ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-user-by-uid [uid ds]
  (-> base-query
      (where-uid uid)
      (sql-format)
      (->> (jdbc/execute-one! ds))))

(defn wrap-find-user [param]
  (fn [handler]
    (fn [{{uid param} :path-params ds :tx :as request}]
      (if-let [user (find-user-by-uid uid ds)]
        (handler (assoc request :user user))
        (sd/response_not_found "No such user.")))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
