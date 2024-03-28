(ns madek.api.resources.media-entries.advanced-filter
  (:require
   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]

         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]

               [madek.api.db.core :refer [get-ds]]

         [madek.api.utils.helper :refer [convert-filter-values convert-map-if-exist array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [logbug.debug :as debug]
   [madek.api.resources.media-entries.advanced-filter.media-files :as media-files]
   [madek.api.resources.media-entries.advanced-filter.meta-data :as meta-data]
   [madek.api.resources.media-entries.advanced-filter.permissions :as permissions]))

(defn filter-by [sqlmap filter-map]


  (let [
        ;filter-map (convert-map-if-exist filter-map)
        ;filter-map (convert-filter-values filter-map)

        p (println ">o> filter-map" filter-map)
        ;p (println ">o> filter-map / !!!! debug=" (-> (media-files/sql-filter-by (:media_files filter-map))
        ;                                            sql-format
        ;                                            ))

        p (println ">o> NOW!!!=" (-> (sql/select :*)
                                     (sql/from [:media_entries "fake_table"])
                                     (meta-data/sql-filter-by (:meta_data filter-map))
                                     sql-format
                                     ))

        ])



  (let [query (-> sqlmap
                  (media-files/sql-filter-by (:media_files filter-map)) ;;ok
                  (permissions/sql-filter-by (:permissions filter-map)) ;;ok
                  (meta-data/sql-filter-by (:meta_data filter-map))
                  ;(meta-data/sql-search-through-all (:search filter-map))
                  )

        ]
    ;(logging/info "filter-by" "\nfilter-map:\n" filter-map "\nresult:" query)
    query))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
