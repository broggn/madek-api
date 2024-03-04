(ns madek.api.resources.media-entries.advanced-filter.media-files
  (:require
   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   ;[madek.api.utils.sql :as sql]
   
         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]
               
               [madek.api.db.core :refer [get-ds]]
               
         [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]
   ))

(defn- sql-merge-where-media-file-spec [sqlmap media-file-spec]
  (-> sqlmap
      (sql/where
       [:=
        (keyword (str "media_files." (:key media-file-spec)))
        (:value media-file-spec)])))

(defn sql-filter-by [sqlmap media-file-specs]
  (if-not (empty? media-file-specs)
    (reduce sql-merge-where-media-file-spec
            (-> sqlmap
                (sql/join
                 :media_files
                 [:= :media_files.media_entry_id :media_entries.id]))
            media-file-specs)
    sqlmap))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
