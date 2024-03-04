(ns madek.api.resources.media-entries.media-entry
  (:require
   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   [logbug.debug :as debug]

   [madek.api.utils.helper :refer [to-uuid]]
   [madek.api.utils.rdbms :as rdbms]
   ;[madek.api.utils.sql :as sql]
   
         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]
               
               [madek.api.db.core :refer [get-ds]]
               
         [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]
   ))

(def ^:private media-entry-keys
  [:id :created_at
   :creator_id
   :responsible_user_id
   :is_published
   :updated_at :edit_session_updated_at :meta_data_updated_at])

(defn get-media-entry-for-preview [request]
  (let [preview-id (or (-> request :params :preview_id) (-> request :parameters :path :preview_id))
        query (-> (sql/select :*)
                  (sql/from :media_entries)
                  (sql/join :media_files [:= :media_entries.id :media_files.media_entry_id])
                  (sql/join :previews [:= :media_files.id :previews.media_file_id])
                  (sql/where [:= :previews.id (to-uuid preview-id)])
                  (sql-format))
        dbresult  (jdbc/execute-one! (get-ds) query)]
    ;(logging/info "get-media-entry-for-preview" "\npreview-id\n" preview-id "\ndbresult\n" dbresult)
    dbresult))

(defn get-media-entry [request]
  (when-let [media-entry (:media-resource request)]
    {:body (select-keys media-entry media-entry-keys)}))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
