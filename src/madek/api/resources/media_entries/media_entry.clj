(ns madek.api.resources.media-entries.media-entry
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [drtom.logbug.debug :as debug]
    [madek.api.resources.media-resources.media-resource :as media-resource]
    ))

(def ^:private media-entry-keys
  (conj media-resource/media-resource-keys :is_published))

(defn get-media-entry [request]
  (media-resource/get-media-resource request
                                     :table :media_entries
                                     :mr-keys media-entry-keys
                                     :mr-type "MediaEntry"))

;### Debug ####################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
