(ns madek.api.resources.collections.advanced-filter.permissions
  (:require
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   ;[madek.api.utils.sql :as sql]

   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

;(defn- api-client-authorized-condition [perm id]
;  [:or
;   [:= (keyword (str "collections." perm)) true]
;   [:exists (-> (sql/select true)
;                (sql/from [:collection_api_client_permissions :cacp])
;                (sql/where [:= :cacp.collection_id :collections.id])
;                (sql/where [:= (keyword (str "cacp." perm)) true])
;                (sql/where [:= :cacp.api_client_id id]))]])

(defn- group-permission-exists-condition [perm id]
  [:exists (-> (sql/select true)
               (sql/from [:collection_group_permissions :cgp])
               (sql/where [:= :cgp.collection_id :collections.id])
               (sql/where [:= (keyword (str "cgp." perm)) true])
               (sql/where [:= :cgp.group_id id]))])

(defn- user-permission-exists-condition [perm id]
  [:exists (-> (sql/select true)
               (sql/from [:collection_user_permissions :cup])
               (sql/where [:= :cup.collection_id :collections.id])
               (sql/where [:= (keyword (str "cup." perm)) true])
               (sql/where [:= :cup.user_id id]))])

(defn- group-permission-for-user-exists-condition [perm id]
  [:exists (-> (sql/select true)
               (sql/from [:collection_group_permissions :cgp])
               (sql/where [:= :cgp.collection_id :collections.id])
               (sql/where [:= (keyword (str "cgp." perm)) true])
               (sql/join :groups
                 [:= :groups.id :cgp.group_id])
               (sql/join [:groups_users :gu]
                 [:= :gu.group_id :groups.id])
               (sql/where [:= :gu.user_id id]))])

(defn- user-authorized-condition [perm id]
  [:or
   ;[:= (keyword (str "collections." perm)) true]
   (when (= perm "get_metadata_and_previews")
     [:= (keyword (str "collections." perm)) true])

   [:= :collections.responsible_user_id id]

   (when (or (= perm "get_metadata_and_previews")
           (= perm "edit_metadata_and_relations")
           (= perm "edit_permissions"))
     (user-permission-exists-condition perm id))

   (when (or (= perm "get_metadata_and_previews")
           (= perm "edit_metadata_and_relations"))
     (group-permission-for-user-exists-condition perm id))])

;(defn- filter-by-permission-for-auth-entity [sqlmap permission authenticated-entity]
;
;
;  (try
;    (println ">o> filter-by-permission-for-auth-entity authenticated-entity/type=" (:type authenticated-entity))
;    (println ">o> filter-by-permission-for-auth-entity authenticated-entity/entity=" authenticated-entity)
;    (println ">o> filter-by-permission-for-auth-entity permission=" permission)
;
;    (catch Exception ex
;      (println ">o> filter-by-permission-for-auth-entity ERROR")
;      )
;    )
;
;  (when-not authenticated-entity
;    (do
;      (println ">o> CAUTION/FIX-ME !!!!!!!!!!!!!!")
;      sqlmap)
;    (case (:type authenticated-entity)
;      "User" (sql/where sqlmap (user-authorized-condition
;                                 permission (:id authenticated-entity)))
;
;
;      ; TODO session
;      ;"ApiClient" (sql/where sqlmap (api-client-authorized-condition
;      ;                                      permission (:id authenticated-entity)))
;      (throw (ex-info (str "Filtering for " permission " requires a signed-in entity.")
;               {:status 422})))
;    )
;  )

(defn- filter-by-permission-for-auth-entity [sqlmap permission authenticated-entity]
  (case (:type authenticated-entity)
    "User" (sql/where sqlmap (user-authorized-condition
                                     permission (:id authenticated-entity)))
    ; TODO session
    ;"ApiClient" (sql/merge-where sqlmap (api-client-authorized-condition
    ;                                      permission (:id authenticated-entity)))
    (throw (ex-info (str "Filtering for " permission " requires a signed-in entity.")
             {:status 422}))))

(defn filter-by-query-params [sqlmap query-params authenticated-entity]

  ;(let [
  ;      p (println ">o> query-params=" query-params)
  ;
  ;      true_param ["me_get_metadata_and_previews"
  ;                  "me_edit_metadata_and_relations"
  ;                  "me_edit_permission"]
  ;
  ;      p (println ">o> true_param" true_param)
  ;
  ;      res1 (get query-params (keyword true_param))
  ;      p (println ">o> res1=" res1)
  ;
  ;      res2 (not= (get query-params (keyword true_param)) true)
  ;      p (println ">o> res2=" res2)
  ;
  ;
  ;      ;>o> query-params= {:me_edit_metadata_and_relations true, :collection_id #uuid "b71e14b7-f5c4-4166-81e5-d51090d13232"}
  ;      ;>o> true_param [me_get_metadata_and_previews me_edit_metadata_and_relations me_edit_permission]
  ;      ;>o> res1= nil
  ;      ;>o> res2= true
  ;      ;>o> filter-by-query-params
  ;      ;>o> authenticated-entity/type= nil
  ;
  ;      ])



  (doseq [true_param ["me_get_metadata_and_previews"
                      "me_edit_metadata_and_relations"
                      "me_edit_permission"]]
    (when (contains? query-params (keyword true_param))
      (when (not= (get query-params (keyword true_param)) true)
        (throw (ex-info (str "Value of " true_param " must be true when present.")
                 {:status 422})))))

  (println ">o> filter-by-query-params")
  (cond-> sqlmap
    (:public_get_metadata_and_previews query-params)
    (sql/where [:= :collections.get_metadata_and_previews true])
    (= (:me_get_metadata_and_previews query-params) true)
    (filter-by-permission-for-auth-entity "get_metadata_and_previews" authenticated-entity)
    ; 
    (= (:me_edit_metadata_and_relations query-params) true)
    (filter-by-permission-for-auth-entity "edit_metadata_and_relations" authenticated-entity)
    ;
    (= (:me_edit_permission query-params) true)
    (filter-by-permission-for-auth-entity "edit_permission" authenticated-entity)))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
