(ns madek.api.resources.users.main
  (:require
   [logbug.debug :as debug]
   [madek.api.resources.users.create :as create-user]
   [madek.api.resources.users.delete :as delete-user]
   [madek.api.resources.users.get :as get-user]
   [madek.api.resources.users.index :as index]
   [madek.api.resources.users.update :as update-user]
   [madek.api.utils.logging :as logging]
   [taoensso.timbre :refer [debug error info spy warn]]))

; There are some things missing here yet. A non admin user should be able to
; get limited users set (by properties and number of resutls). The index for
; admins misses useful query params.
; This is pending because of possible future changes of the relation between
; the users and the people table.

;### routes ###################################################################

(def admin-routes
  ["/users"
   {:swagger {:tags ["admin/users"] :security [{"auth" []}]}}
   ["/"
    {:get index/route
     :post create-user/route}]
   ["/:id"
    {:get get-user/route
     :delete delete-user/route
     :patch update-user/route}]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
