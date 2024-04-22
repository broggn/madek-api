(ns madek.api.resources.collections.permissions
  (:require
   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.thrown :as thrown]
   [madek.api.resources.media-resources.permissions :as mr-permissions]))

(defn viewable-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/viewable-by-auth-entity?
   resource auth-entity "collection" tx))

(defn editable-meta-data-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/permission-by-auth-entity?
   resource auth-entity :edit_metadata_and_relations "collection" tx))

(defn editable-permissions-by-auth-entity? [resource auth-entity tx]
  (mr-permissions/edit-permissions-by-auth-entity?
   resource auth-entity "collection" tx))
