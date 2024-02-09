(ns madek.api.resources.groups
  (:require [clj-uuid]
            [clojure.java.jdbc :as jdbco]
            [clojure.tools.logging :as logging]
            [honey.sql :refer [format] :rename {format sql-format}]
   ;; all needed imports
            [honey.sql.helpers :as sql]
            [madek.api.db.core :refer [get-ds]]
            [madek.api.pagination :as pagination]
   ;[madek.api.resources.groups.users :as users]
            [madek.api.resources.groups.shared :as groups]
            [madek.api.resources.groups.users :as group-users]

            [madek.api.resources.shared :as sd]
            [madek.api.utils.auth :refer [wrap-authorize-admin!]]



            [madek.api.utils.rdbms :as rdbms]
            [madek.api.utils.sql :as sqlo]
            [next.jdbc :as jdbc]




            [reitit.coercion.schema]
            [schema.core :as s]))

;### create group #############################################################

(defn create-group [request]
  (let [params (as-> (:body request) params
                 (or params {})
                 (assoc params :id (or (:id params) (clj-uuid/v4))))

        p (println ">o> params" params)
        ]

    {:body (dissoc
             (->> (jdbc/execute-one! (get-ds) (-> (sql/insert-into :groups)
                                                  (sql/values [params])
                                                  (sql/returning :*)
                                                  sql-format
                                                  )))
             :previous_id :searchable)
     :status 201}))


;(ns leihs.my.back.html
;    (:refer-clojure :exclude [keyword str])
;    (:require
;      [hiccup.page :refer [html5]]
;      [honey.sql :refer [format] :rename {format sql-format}]
;      [honey.sql.helpers :as sql]
;      [leihs.core.http-cache-buster2 :as cache-buster]
;      [leihs.core.json :refer [to-json]]
;      [leihs.core.remote-navbar.shared :refer [navbar-props]]
;      [leihs.core.shared :refer [head]]
;      [leihs.core.url.core :as url]
;      [leihs.my.authorization :as auth]
;      [leihs.core.db :as db]
;      [next.jdbc :as jdbc]))



;(defn merge-query-parts [query-parts]
;  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
;
;        p (println ">o> placeholder-count" placeholder-count)
;
;        ;required-entries (+ 1 placeholder-count)
;        required-entries (- (count query-parts) placeholder-count)
;        p (println ">o> required-entries" required-entries)
;
;        merged (apply str (take required-entries query-parts))
;
;        p (println ">o> merged" merged)
;        remaining (drop required-entries query-parts)]
;    (cons merged remaining)))

(defn merge-query-parts [query-parts]
  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
        ;; Print placeholder count for debugging
        _ (println ">o> placeholder-count" placeholder-count)
        ;; Calculate required entries
        required-entries (- (count query-parts) placeholder-count)
        ;; Print required entries for debugging
        _ (println ">o> required-entries" required-entries)
        ;; Merge with spaces between entries
        merged (vector (apply str (interpose " " (take required-entries query-parts))))

        merged ["select * from groups where institutional_id = ?"]

        ;; Print merged result for debugging
        _ (println ">o> merged" merged)
        _ (println ">o> merged" (count merged))
        _ (println ">o> merged" (class merged))
        ;; Calculate remaining parts
        remaining (vector (drop required-entries query-parts))
        ;remaining (vector (drop required-entries query-parts))

        remaining ["ab"]

        ]
    _ (println ">o> 2merged" remaining)
    _ (println ">o> 2merged" (count remaining))
    _ (println ">o> 2merged" (class remaining))
    ;; Combine merged with remaining parts
    (concat merged remaining)))
;(+ merged remaining)))
;(cons merged remaining)))


;(defn merge-query-parts [query-parts]
;  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
;        _ (println ">o> placeholder-count" placeholder-count)
;        required-entries (- (count query-parts) placeholder-count)
;        _ (println ">o> required-entries" required-entries)
;        merged (apply str (interpose " " (take required-entries query-parts)))
;        _ (println ">o> merged" merged)
;        remaining (drop required-entries query-parts)]
;    (vector merged @remaining))) ; Change here to ensure the result is a vector of strings

(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [


        id "ab"

        fir (-> (sql/delete-from :groups)

                ;(sql/where (groups/jdbc-update-group-id-where-clause id))

                sql-format)
        p (println ">o> abc fir" fir)

        sec (groups/jdbc-update-group-id-where-clause id)
        p (println ">o> abc sec" sec)

        p (println ">o> abc both" fir sec)

        both (concat fir sec)
        p (println ">o> abc both" both)
        p (println ">o> abc both" (count both))

        ;both (vector (merge-query-parts both))
        merged  (merge-query-parts both)

        res (jdbc/execute! (get-ds) merged)
        p (println ">o> res=" res)

        ]

      res
    )

  )


(comment

  (let [
        params {
                :name "test22"
                :type "Group"
                :institutional_id "test22"
                :institutional_name "test22"
                :institution "test22"
                :searchable "test"
                }

        ;; java.jdbc
        ;res (->> (jdbco/insert!
        ;       (rdbms/get-ds) :groups params)
        ;  fir)

        ;(fir (jdbco/delete! (rdbms/get-ds)
        ;         :groups (groups/jdbc-update-group-id-where-clause id)))

        ;; next.jdbc
        ;    [madek.api.db.core :refer [get-ds]]
        ;    [honey.sql :refer [format] :rename {format sql-format}]
        ;res (->> (jdbc/execute-one! (get-ds) (-> (sql/insert-into :groups)
        ;                                         (sql/values [params])
        ;                                         (sql/returning :*)
        ;                                         sql-format
        ;                                         )))

        id "e3e3e3e3-e3e3-e3e3-e3e3-e3e3e3e3e3e3"


        id "ab"

        sql (groups/jdbc-update-group-id-where-clause id)
        p (println ">o> sql" sql)
        p (println ">o> sql" (class sql))

        ;res (jdbco/delete! (rdbms/get-ds)
        ;  :groups sql)

        ;(def query ("DELETE FROM groups" " WHERE groups.institutional_id = ?" "ab"))



        fir (-> (sql/delete-from :groups)

                  ;(sql/where (groups/jdbc-update-group-id-where-clause id))

                  sql-format)
        p (println ">o> abc fir" fir)

        ;sec (groups/jdbc-update-group-id-where-clause id)
        ;p (println ">o> abc sec" sec)
        ;
        ;p (println ">o> abc both" fir sec)
        ;
        ;both (concat fir sec)
        ;p (println ">o> abc both" both)
        ;p (println ">o> abc both" (count both))
        ;
        ;both (vector (merge-query-parts both))
        ;both  (merge-query-parts both)
        ;

        both ["DELETE FROM groups WHERE groups.institutional_id = ?" "ab"]

        ;
        p (println ">>1" both)
        p (println ">>1 count=" (count both))
        ;p (println ">>1 fir=" (fir both))

        ;p (println ">o> abc" (groups/jdbc-update-group-id-where-clause id))
        ;p (println ">o> abc" (class (groups/jdbc-update-group-id-where-clause id)))







        ;; TODO: one
        ;res (jdbc/execute-one! (get-ds) both)
        ;p (println ">> !! res=" res)
        ;
        ;result-map (get  res :next.jdbc/update-count)
        ;p (println ">> !! a1a=" result-map)

        ;; TODO: all
        res (jdbc/execute! (get-ds) both)
        p (println ">> !! res=" res)

        result-map (get  (first res) :next.jdbc/update-count)
        res (get  (first res) :next.jdbc/update-count)

        p (println ">> !! final-result=" res)









        ;;p (println ">> !! a1=" (res [0]))
        ;p (println ">> !! a1=" (fir res))
        ;p (println ">> !! a1=" (::jdbc/update-count (fir res)))
        ;
        ;
        ;
        ;
        ;count (-> (fir res) :next.jdbc/update-count)
        ;p (println ">> !! count=" count)

        ;res (jdbc/execute! (get-ds) fir (groups/jdbc-update-group-id-where-clause id) )
        ;res (jdbc/execute-one! (get-ds)  (-> (sql/delete-from :groups)
        ;                                     (sql/where groups/jdbc-update-group-id-where-clause id)
        ;                                                                              sql-format
        ;                                     ))

        ;res sql

        ]
    res
    )                                                       ;;HERE
  )




(comment

  ;(def query ["DELETE FROM groups" " WHERE groups.institutional_id = ?" "ab"])



  ;; Example usage

  (let [

        query ["DELETE FROM groups" " WHERE groups.institutional_id = ?" "ab"]
        ;query ["DELETE FROM groups" " WHERE groups.institutional_id = ?" " AND x=? And a between(? ?)" "ab" "cd" "1" "4"]

        res (merge-query-parts query)

        p (println ">>1" res)
        p (println ">>1 count=" (count res))
        p (println ">>1 first=" (first res))

        ]
    )
  )


;### get group ################################################################

(defn get-group [id-or-institutinal-group-id]
  (if-let [group (groups/find-group id-or-institutinal-group-id)]
    {:body (dissoc group :previous_id :searchable)}
    {:status 404 :body "No such group found"}))             ; TODO: toAsk 204 No Content

;### delete group ##############################################################

(defn delete-group [id]
  (if (= 1 (first (jdbco/delete! (rdbms/get-ds)
                    :groups (groups/jdbc-update-group-id-where-clause id))))
    {:status 204}
    {:status 404}))

;### patch group ##############################################################
(defn db_update-group [group-id body]
  (let [query (groups/jdbc-update-group-id-where-clause group-id)
        db-do (jdbco/update! (rdbms/get-ds) :groups body query)]
    ;(logging/info "db_update-group" "\ngroup-id\n" group-id "\nbody\n" body "\nquery\n" query)
    (first db-do)))

(defn patch-group [{body :body {group-id :group-id} :params}]
  (if (= 1 (db_update-group group-id body))
    {:body (groups/find-group group-id)}
    {:status 404}))

;### index ####################################################################
; TODO test query and paging
(defn build-index-query [req]
  (let [query-params (-> req :parameters :query)]
    (-> (if (true? (:full_data query-params))
          (sqlo/select :*)
          (sqlo/select :id))
        (sqlo/from :groups)
        (sqlo/order-by [:id :asc])
        (sd/build-query-param query-params :id)
        (sd/build-query-param query-params :institutional_id)
        (sd/build-query-param query-params :type)
        (sd/build-query-param query-params :person_id)

        (sd/build-query-param-like query-params :name)
        (sd/build-query-param-like query-params :institutional_name)
        (sd/build-query-param-like query-params :institution)
        (sd/build-query-param-like query-params :searchable)
        (pagination/add-offset-for-honeysql query-params)
        sqlo/format)))

(defn index [request]
  (let [result (jdbco/query (rdbms/get-ds) (build-index-query request))]
    (sd/response_ok {:groups result})))

;### routes ###################################################################

(def schema_import-group
  {(s/optional-key :id) s/Str
   :name s/Str
   ;:type (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :type) (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)
   (s/optional-key :person_id) (s/maybe s/Uuid)})

(def schema_update-group
  {(s/optional-key :name) s/Str
   (s/optional-key :type) (s/enum "Group" "AuthenticationGroup" "InstitutionalGroup")
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)
   (s/optional-key :person_id) (s/maybe s/Uuid)})

(def schema_export-group
  {:id s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :type) s/Str                             ; TODO enum
   (s/optional-key :created_by_user_id) (s/maybe s/Uuid)
   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any
   (s/optional-key :institutional_id) (s/maybe s/Str)
   (s/optional-key :institutional_name) (s/maybe s/Str)
   (s/optional-key :institution) (s/maybe s/Str)
   (s/optional-key :person_id) (s/maybe s/Uuid)
   (s/optional-key :searchable) s/Str})

(defn handle_create-group
  "TODO  catch errors"
  [request]
  (let [params (get-in request [:parameters :body])
        data_wid (assoc params :id (or (:id params) (clj-uuid/v4)))
        data_wtype (assoc data_wid :type (or (:type data_wid) "Group"))
        resultdb (->> (jdbco/insert! (rdbms/get-ds) :groups data_wtype) first)
        result (dissoc resultdb :previous_id :searchable)]
    (logging/info (apply str ["handler_create-group: \ndata:" data_wtype "\nresult-db: " resultdb "\nresult: " result]))
    ;{:status 201 :body {:id result}}
    {:status 201 :body result}))

(defn handle_get-group [req]
  (let [id (-> req :parameters :path :id)]
    (logging/info "handle_get-group" "\nid\n" id)
    (get-group id)))

(defn handle_delete-group [req]
  (let [id (-> req :parameters :path :id)]
    (delete-group id)))

(defn handle_update-group [req]
  (let [id (-> req :parameters :path :id)
        body (-> req :parameters :body)]
    ;(logging/info "handle_update-group" "\nid\n" id "\nbody\n" body)
    (patch-group {:params {:group-id id} :body body})))

(def schema_query-groups
  {(s/optional-key :id) s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :type) s/Str
   (s/optional-key :created_at) s/Any
   (s/optional-key :updated_at) s/Any
   (s/optional-key :institutional_id) s/Str
   (s/optional-key :institutional_name) s/Str
   (s/optional-key :institution) s/Str
   (s/optional-key :person_id) s/Uuid
   (s/optional-key :searchable) s/Str
   (s/optional-key :full_data) s/Bool
   (s/optional-key :page) s/Int
   (s/optional-key :count) s/Int})

(def user-routes
  [["/groups"
    ["/" {:get {:summary "Get all group ids"
                :description "Get list of group ids. Paging is used as you get a limit of 100 entries."
                :handler index
                :middleware [wrap-authorize-admin!]
                :swagger {:produces "application/json"}
                :content-type "application/json"
                :parameters {:query schema_query-groups}
                ;:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :responses {200 {:body {:groups [schema_export-group]}}}}}]
    ["/:id" {:get {:summary "Get group by id"
                   :description "Get group by id. Returns 404, if no such group exists."
                   :swagger {:produces "application/json"}
                   :content-type "application/json"
                   :handler handle_get-group
                   :middleware [wrap-authorize-admin!]
                   :coercion reitit.coercion.schema/coercion
                   :parameters {:path {:id s/Str}}
                   :responses {200 {:body schema_export-group}
                               404 {:body s/Any}}}}]]])


;; api/admin/..
(def ring-routes
  ["/groups"
   ["/" {:get {:summary "Get all group ids"
               :description "Get list of group ids. Paging is used as you get a limit of 100 entries."
               :handler index
               :middleware [wrap-authorize-admin!]
               :swagger {:produces "application/json"}
               :parameters {:query schema_query-groups}
               :content-type "application/json"
               :coercion reitit.coercion.schema/coercion
               :responses {200 {:body {:groups [schema_export-group]}}}}

         :post {:summary "Create a group"
                :description "Create a group."
                :handler handle_create-group
                :middleware [wrap-authorize-admin!]
                :swagger {:produces "application/json" :consumes "application/json"}
                :content-type "application/json"
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :parameters {:body schema_import-group}
                :responses {201 {:body schema_export-group}
                            500 {:body s/Any}}}}]

   ["/:id" {:get {:summary "Get group by id"
                  :description "Get group by id. Returns 404, if no such group exists."
                  :swagger {:produces "application/json"}
                  :content-type "application/json"
                  :accept "application/json"
                  :handler handle_get-group
                  :middleware [wrap-authorize-admin!]
                  :coercion reitit.coercion.schema/coercion
                  :parameters {:path {:id s/Str}}
                  :responses {200 {:body schema_export-group}
                              404 {:body s/Any}}}

            :delete {:summary "Deletes a group by id"
                     :description "Delete a group by id"
                     :handler handle_delete-group
                     :middleware [wrap-authorize-admin!]
                     :coercion reitit.coercion.schema/coercion
                     :parameters {:path {:id s/Str}}
                     :responses {403 {:body s/Any}
                                 204 {:body s/Any}}}

            :put {:summary "Get group by id"
                  :description "Get group by id. Returns 404, if no such group exists."
                  :swagger {:produces "application/json"}
                  :content-type "application/json"
                  :accept "application/json"
                  :handler handle_update-group
                  :middleware [wrap-authorize-admin!]
                  :coercion reitit.coercion.schema/coercion
                  :parameters {:path {:id s/Str}
                               :body schema_update-group}
                  :responses {200 {:body s/Any}             ;groups/schema_export-group}
                              404 {:body s/Any}}}}]         ; TODO error handling

   ; groups-users/ring-routes
   ["/:group-id/users/" {:get {:summary "Get group users by id"
                               :description "Get group users by id."
                               :swagger {:produces "application/json"}
                               :content-type "application/json"

                               :handler group-users/handle_get-group-users
                               :middleware [wrap-authorize-admin!]
                               :coercion reitit.coercion.schema/coercion
                               :parameters {:path {:group-id s/Str}
                                            :query {(s/optional-key :page) s/Int
                                                    (s/optional-key :count) s/Int}}
                               :responses {200 {:body {:users [group-users/schema_export-group-user-simple]}} ; TODO schema
                                           404 {:body s/Str}}}

                         ; TODO works with tests, but not with the swagger ui
                         :put {:summary "Update group users by group-id and list of users."
                               :description "Update group users by group-id and list of users."
                               :swagger {:consumes "application/json" :produces "application/json"}
                               :content-type "application/json"
                               :accept "application/json"
                               :handler group-users/handle_update-group-users
                               :coercion reitit.coercion.schema/coercion
                               :parameters {:path {:group-id s/Str}
                                            :body group-users/schema_update-group-user-list}

                               ;:body {:users [s/Any]}}
                               :responses {200 {:body s/Any} ;groups/schema_export-group}
                                           404 {:body s/Str}}}}]

   ["/:group-id/users/:user-id" {:get {:summary "Get group user by group-id and user-id"
                                       :description "Get group user by group-id and user-id."
                                       :swagger {:produces "application/json"}
                                       :content-type "application/json"
                                       :handler group-users/handle_get-group-user
                                       :middleware [wrap-authorize-admin!]
                                       :coercion reitit.coercion.schema/coercion
                                       :parameters {:path {:group-id s/Str :user-id s/Str}}
                                       :responses {200 {:body group-users/schema_export-group-user-simple}
                                                   404 {:body s/Any}}} ; TODO error handling

                                 :put {:summary "Get group user by group-id and user-id"
                                       :description "Get group user by group-id and user-id."
                                       :swagger {:produces "application/json"}
                                       :content-type "application/json"
                                       :handler group-users/handle_add-group-user
                                       :middleware [wrap-authorize-admin!]
                                       :coercion reitit.coercion.schema/coercion
                                       :parameters {:path {:group-id s/Str :user-id s/Str}}
                                       :responses {200 {:body {:users [group-users/schema_export-group-user-simple]}}
                                                   404 {:body s/Any}}} ; TODO error handling

                                 :delete {:summary "Deletes a group-user by group-id and user-id"
                                          :description "Delete a group-user by group-id and user-id."
                                          ;:swagger {:produces "application/json"}
                                          ;:content-type "application/json"
                                          :handler group-users/handle_delete-group-user
                                          :middleware [wrap-authorize-admin!]
                                          :coercion reitit.coercion.schema/coercion
                                          :parameters {:path {:group-id s/Str :user-id s/Str}}
                                          :responses {200 {:body {:users [group-users/schema_export-group-user-simple]}}
                                                      406 {:body s/Str}}}}] ; TODO error handling
   ])
;### Debug ####################################################################
;(debug/debug-ns *ns*)
