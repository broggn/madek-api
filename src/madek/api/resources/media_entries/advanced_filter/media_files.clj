(ns madek.api.resources.media-entries.advanced-filter.media-files
  (:require
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   ;[madek.api.utils.helper :refer [convert-map-if-exist]]

   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.utils.helper :refer [convert-map-if-exist array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

(defn- sql-merge-where-media-file-spec [sqlmap media-file-spec]



  (let [
        key (:key media-file-spec)
        key-prefixed (keyword (str "media_files." key))
        val (:value media-file-spec)
        p (println ">o> val.cl before" (class val))
        val (to-uuid val key)
        p (println ">o> val.cl after" (class val))
        ]

    (println ">o> sql-merge-where-media-file-spec???? key=" key-prefixed)
    (println ">o> sql-merge-where-media-file-spec???? val=" val)
    (println ">o> --------------------------------------")

    (-> sqlmap
        (sql/where [:= key-prefixed val]))
    )

  )

(defn sql-filter-by [sqlmap media-file-specs]
  (if-not (empty? media-file-specs)
    (reduce sql-merge-where-media-file-spec
      (-> sqlmap
          (sql/join
            :media_files
            [:= :media_files.media_entry_id :media_entries.id]))
      media-file-specs)
    ;(convert-map-if-exist media-file-specs))
    sqlmap))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
