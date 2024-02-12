(ns madek.api.resources.previews.index
  (:require
   [clojure.java.jdbc :as jdbco]
   [clojure.tools.logging :as logging]

         ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]

   [madek.api.db.core :refer [get-ds]]

   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sqlo]
   [next.jdbc :as jdbc]))

(defn- get-first-or-30-percent [list]
  (if (> (count list) 1)
    (nth list (min (Math/ceil (* (/ (count list) 10.0) 3)) (- (count list) 1)))
    (first list)))

(defn- detect-ui-preview-id [sqlmap media-type]
  (if (= media-type "video")

;(let [query (-> sqlmap (sql/merge-where [:= :media_type "image"])
    ;                (sql/merge-where [:= :thumbnail "large"])
    ;                (sql/order-by [:previews.filename :asc] [:previews.created_at :desc]))]
  ;  (let [previews (jdbc/query (rdbms/get-ds) (sql/format query))]
  ;    (:id (get-first-or-30-percent previews))))
  ;nil))

    (let [query (-> sqlmap
                    (sql/where [:= :media_type "image"]
                               [:= :thumbnail "large"])
                    (sql/order-by [:previews.filename :asc] [:previews.created_at :desc])
                    (sql-format))]
      (let [previews (jdbc/execute! (get-ds) query)]
        (:id (get-first-or-30-percent previews))))
    nil))

(defn- add-preview-pointer-to [previews detected-id]
  (map #(if (= (:id %) detected-id) (assoc % :used_as_ui_preview true) %) previews))

(defn get-index [media-file]
  ;(let [sqlmap (-> (sql/select :previews.*)
  ;                 (sql/from :previews)
  ;                 (sql/merge-where
  ;                  [:= :previews.media_file_id (:id media-file)])
  ;                 (sql/order-by [:previews.created_at :desc]))]
  ;  (let [detected-id (detect-ui-preview-id sqlmap (:media_type media-file))]
  ;    (add-preview-pointer-to
  ;     (jdbc/query (rdbms/get-ds) (sql/format sqlmap))
  ;     detected-id))))

  (let [sqlmap (-> (sql/select :previews.*)
                   (sql/from :previews)
                   (sql/where [:= :previews.media_file_id (:id media-file)])
                   (sql/order-by [:previews.created_at :desc])
                   (sql-format))]
    (let [detected-id (detect-ui-preview-id sqlmap (:media_type media-file))]
      (add-preview-pointer-to
       (jdbc/execute! (get-ds) sqlmap)
       detected-id))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
