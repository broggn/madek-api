(ns madek.api.resources.people.main
  (:require
   [logbug.debug :as debug]
   [madek.api.resources.people.create :as create-person]
   [madek.api.resources.people.delete :as delete-person]
   [madek.api.resources.people.get :as get-person]
   [madek.api.resources.people.index :as index]
   [madek.api.resources.people.update :as update-person]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def user-routes
  ["/people"
   {:swagger {:tags ["people"] }}
   ["/"
    {;:get index/route
     }]
   ["/:id"
    {:get get-person/route}]])

(def admin-routes
  ["/people"
   {:swagger {:tags ["admin/people"] :security [{"auth" []}]}}
   ["/"
    {:get index/route
     :post create-person/route}]
   ["/:id"
    {:get get-person/route
     :patch update-person/route
     :delete delete-person/route}]])
