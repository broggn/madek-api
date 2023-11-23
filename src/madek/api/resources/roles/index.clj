(ns madek.api.resources.roles.index
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   [logbug.debug :as debug]
   [madek.api.pagination :as pagination]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.rdbms :as rdbms]
   [madek.api.utils.sql :as sql]))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
