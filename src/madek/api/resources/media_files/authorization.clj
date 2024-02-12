(ns madek.api.resources.media-files.authorization
  (:require
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.media-entries.permissions :as me-permissions]
   [next.jdbc :as jdbc]))

(defn- media-file-authorize [request handler scope]
  ;(let [media-entry-id (get-in request [:media-file :media_entry_id])
  ;      media-entry (-> (jdbc/query (get-ds)
  ;                                  [(str "SELECT * FROM media_entries WHERE id = ?")
  ;                                   media-entry-id]) first)]

  (let [media-entry-id (get-in request [:media-file :media_entry_id])
        query (-> (sql/select :*)
                  (sql/from :media_entries)
                  (sql/where [:= :id media-entry-id])
                  (sql-format))
        media-entry (jdbc/execute-one! (get-ds) query)]

    (logging/info "authorize" "\nmedia-entry-id\n" media-entry-id "\nmedia-entry\n" media-entry)
    (if (get media-entry scope)
      (handler request)
      (if-let [auth-entity (:authenticated-entity request)]
        (if (case scope
              :get_full_size (me-permissions/downloadable-by-auth-entity?
                              media-entry auth-entity)
              :get_metadata_and_previews (me-permissions/viewable-by-auth-entity?
                                          media-entry auth-entity))
          (handler request)
          {:status 403})
        {:status 401}))))

(defn wrap-auth-media-file-metadata-and-previews [handler]
  (fn [request] (media-file-authorize request handler :get_metadata_and_previews)))

(defn wrap-auth-media-file-full_size [handler]
  (fn [request] (media-file-authorize request handler :get_full_size)))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
