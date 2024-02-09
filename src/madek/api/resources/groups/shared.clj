(ns madek.api.resources.groups.shared
  (:require
   [clj-uuid]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.utils.helper :refer [to-uuid]]
   [next.jdbc :as jdbc]))

(defn sql-merge-where-id
  ([group-id] (println ">> 1") (sql-merge-where-id {} group-id))

  ([sql-map group-id]
   (println ">> 2" sql-map group-id)
   (if (re-matches
        #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        group-id)
     (sql/where sql-map [:or
                         [:= :groups.id (to-uuid group-id)]
                         [:= :groups.institutional_id group-id]])
     (sql/where sql-map [:= :groups.institutional_id group-id]))))

(defn jdbc-update-group-id-where-clause [id]
  (println ">> 3")
  (-> id sql-merge-where-id
      ;sql-format
      ;(update-in [0] #(clojure.string/replace % "WHERE" ""))
      ))
(defn jdbc-update-group-id-where-clause-old [id]
  (println ">> 3")
  (-> id sql-merge-where-id sql-format
      (update-in [0] #(clojure.string/replace % "WHERE" ""))))

(defn find-group-sql [id]
  (-> (sql-merge-where-id id)
      (sql/select :*)
      (sql/from :groups)
      sql-format))

; TODO: remove this
;(def builder-fn-options
;  {:builder-fn next.jdbc.result-set/as-unqualified-lower-maps})

(defn find-group [id]
  ; TODO: works correctly
  (jdbc/execute-one! (get-ds) (find-group-sql id))

  ; TODO: BROKEN: "groups/institution": "local",
  ;(jdbc/execute-one! (rdbms/get-ds) (find-group-sql id))

  ; TODO: works correctly
  ;(jdbc/execute-one! (rdbms/get-ds) (find-group-sql id) builder-fn-options)
  )
