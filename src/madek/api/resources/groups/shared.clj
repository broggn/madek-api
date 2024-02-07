(ns madek.api.resources.groups.shared
  (:require
   [clj-uuid]
   [clojure.java.jdbc :as jdbco]
   [clojure.tools.logging :as logging]
   [logbug.debug :as debug]

   [madek.api.utils.rdbms :as rdbms]

   [madek.api.db.core :refer [get-ds]]

   ;[madek.api.db.core :refer [get-ds]]

   [madek.api.utils.sql :as sqlo]

   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]

   ))



(defn sql-merge-where-id
  ([group-id] (sql-merge-where-id {} group-id))
  ([sql-map group-id]
   (if (re-matches
         #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
         group-id)
     (sql/where sql-map [:or
                               [:= :groups.id [:cast group-id :uuid]]
                               [:= :groups.institutional_id group-id]])
     (sql/where sql-map [:= :groups.institutional_id group-id]))))

(defn jdbc-update-group-id-where-clause [id]
  (println ">o> jdbc-update-group-id-where-clause" id)
  (-> id sql-merge-where-id sql-format
  ;(-> id sql-merge-where-id (sql-format :inline false)
      (update-in [0] #(clojure.string/replace % "WHERE" ""))))

(defn find-group-sql [id]
  (println ">o> find-group-sql" id)
  (-> (sql-merge-where-id  id )
  ;(-> (sql-merge-where-id (:cast id :uuid))
      (sql/select :*)
      (sql/from :groups)
      ;(sql-format :inline false)))
      sql-format))


(def builder-fn-options
  {:builder-fn next.jdbc.result-set/as-unqualified-lower-maps})

(defn find-group [id]
  ; works correctly
  (jdbc/execute-one! (get-ds) (find-group-sql id))

  ; BROKEN: "groups/institution": "local",
  ;(jdbc/execute-one! (rdbms/get-ds) (find-group-sql id))

  ; works correctly
  ;(jdbc/execute-one! (rdbms/get-ds) (find-group-sql id) builder-fn-options)
  )
