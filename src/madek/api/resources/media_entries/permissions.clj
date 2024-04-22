(ns madek.api.resources.media-entries.permissions
  (:require
   [madek.api.resources.media-resources.permissions :as mr-permissions :only [permission-by-auth-entity? viewable-by-auth-entity?]]))

(defn viewable-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/viewable-by-auth-entity?
   resource auth-entity "media_entry" tx))

(defn downloadable-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/permission-by-auth-entity?
   resource auth-entity :get_full_size "media_entry" tx))

(defn editable-meta-data-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/permission-by-auth-entity?
   resource auth-entity :edit_metadata "media_entry" tx))

(defn editable-permissions-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/edit-permissions-by-auth-entity?
   resource auth-entity "media_entry" tx))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
