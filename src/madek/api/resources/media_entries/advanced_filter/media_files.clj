(ns madek.api.resources.media-entries.advanced-filter.media-files
  (:require
   [honey.sql.helpers :as sql]
   [madek.api.utils.helper :refer [to-uuid]]))

(defn- sql-merge-where-media-file-spec [sqlmap media-file-spec]
  (let [key (:key media-file-spec)
        key-prefixed (keyword (str "media_files." key))
        val (to-uuid (:value media-file-spec) key)]
    (-> sqlmap
        (sql/where [:= key-prefixed val]))))

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
