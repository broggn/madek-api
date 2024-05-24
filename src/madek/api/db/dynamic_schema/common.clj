(ns madek.api.db.dynamic_schema.common
  (:require
   [madek.api.db.dynamic_schema.schema_logger :refer [slog]]
   [schema.core :as s]

   [taoensso.timbre :refer [error]])
  )



(def schema-cache (atom {}))
(def enum-cache (atom {}))
(def validation-cache (atom []))

(defn get-schema [key & [default]]
  (let [val (or (get @schema-cache key default) s/Any)
        ;; TODO: add entry to validation-cache
        _ (if (= val s/Any) (println ">o> get-schema => ANY !!!!!!!!!!!!!!!!!!!!!!!, key=" key))
        ]
    (slog (str "[get-schema] " key "=" val))
    val))

(defn set-schema [key value]
  (slog (str "[set-schema] (" key ") ->" value))

  ;; TODO: remove this
  (if (contains? [
                  :media-entries.schema_export_media_entry
                  :media-entries.schema_export_meta_data
                  :media-files.schema_export_media_file
                  :media-entries.schema_export_preview
                  :media-entries.schema_export_col_arc
                  :media_entries.schema_export-media-entry-perms
                  :media_entry_user_permissions.schema_export-media-entry-user-permission
                  :media_entry_group_permissions.schema_export-media-entry-group-permission
                  :collections-perms.schema_export-collection-perms
                  :collection_user_permissions.schema_export-collection-user-permission
                  :collection_group_permissions.schema_export-collection-group-permission
                  :vocabularies.schema_export-perms_all_vocabulary
                  :vocabularies.vocabulary_user_permissions
                  :vocabularies.schema_export-group-perms
                  ] key) (println ">o> set-schema => ANY ???, key=" (first key)))

  (swap! schema-cache assoc key (into {} value)))

(defn get-enum [key & [default]]
  (let [val (get @enum-cache key default)] val))

(defn set-enum [key value]
  (swap! enum-cache assoc key value))

(defn get-validation-cache []
  @validation-cache)

(defn add-to-validation-cache [new-element]
  (error "[add-to-validation-cache]" new-element)
  ;(errorf "2[add-to-validation-cache]" new-element)
  ;(error "3[add-to-validation-cache] wtf?????????" {})
  ;(warn "4[add-to-validation-cache] wtf?????????" {})

  (swap! validation-cache conj new-element))