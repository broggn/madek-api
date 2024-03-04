(ns api-test-utils
  (:require
   [api-test-data :as td]
   ;[clojure.java.jdbc :as jdbc]
   ;[madek.api.utils.rdbms :as rdbms]
   
         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]
               
               [madek.api.db.core :refer [get-ds]]
               
         [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]
   ))

(defn init-db [dburl]
  (rdbms/initialize dburl)
  (when-let [ds get-ds] ds))

;(defn dbinsert [table data]
;  (->> (jdbc/insert! (get-ds) table data) first))

(defn dbinsert [table data]
  (let [insert-stmt (-> (sql/insert-into table)
                        (sql/values [data])
                        (sql-format))
        result (jdbc/execute! (get-ds) [insert-stmt])]
    (first result)))




;(defn db-del-by-id [table id]
;  (->> (jdbc/delete! (get-ds) table ["(id = ?)" id]) first))

(defn db-del-by-id [table id]
  (let [delete-stmt (-> (sql/delete-from table)
                        (sql/where [:= :id id])
                        (sql-format))
        result (jdbc/execute! (get-ds) [delete-stmt])]
    (first result)))





(defn init-test-person []
  (let [result (dbinsert :people td/person1)] result))

(defn init-test-user []
  (let [result (dbinsert :users td/user1)] result))

(defn init-test-auth []
  (let [result (dbinsert :auth_systems_users td/auth1)] result))

(defn init-test-admin []
  (let [result (dbinsert :admins td/admin1)] result))

(defn del-test-person []
  (when-let [result (db-del-by-id :people td/person1id)] result))

(defn del-test-auth1 []
  (when-let [result (db-del-by-id :auth_systems_users td/auth1id)] result))

(defn del-test-user []
  (when-let [result (db-del-by-id :users td/user1id)] result))

(defn del-test-admin []
  (when-let [result (db-del-by-id :admins td/admin1id)] result))

(defn init-test-person2 []
  (let [result (dbinsert :people td/person2)] result))

(defn init-test-user2 []
  (let [result (dbinsert :users td/user2)] result))

(defn init-test-auth2 []
  (let [result (dbinsert :auth_systems_users td/auth2)] result))

(defn del-test-person2 []
  (when-let [result (db-del-by-id :people td/person2id)] result))

(defn del-test-auth2 []
  (when-let [result (db-del-by-id :auth_systems_users td/auth2id)] result))

(defn del-test-user2 []
  (when-let [result (db-del-by-id :users td/user2id)] result))
