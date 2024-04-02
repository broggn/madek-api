(ns madek.api.resources.media-resources.permissions
  (:require
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [madek.api.utils.helper :refer [array-to-map convert-map-if-exist map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [madek.api.db.core :refer [get-ds]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

(defn- user-table
  [mr-type]
  (keyword (str mr-type "_user_permissions")))

(defn- group-table
  [mr-type]
  (keyword (str mr-type "_group_permissions")))

(defn- resource-key
  [mr-type]
  (keyword (str mr-type "_id")))

;(defn- build-api-client-permissions-query
;  [media-resource-id api-client-id perm-name mr-type]
;  (-> (sql/select :*)
;      (sql/from (keyword (str mr-type "_api_client_permissions")))
;      (sql/where [:= (keyword (str mr-type "_id")) media-resource-id]
;                 [:= :api_client_id api-client-id]
;                 [:= perm-name true])
;      (sql/format)))

;(defn- build-api-client-permission-list-query
;  [media-resource-id mr-type]
;  (-> (sql/select :*)
;      (sql/from (keyword (str mr-type "_api_client_permissions")))
;      (sql/where [:= (keyword (str mr-type "_id")) media-resource-id])
;      (sql/format)))

(defn resource-permission-get-query
  ([media-resource]
   (case (:type media-resource)
     "MediaEntry" (resource-permission-get-query (:id media-resource) "media_entries")
     "Collection" (resource-permission-get-query (:id media-resource) "collections")))

  ([mr-id mr-table]
   (-> (jdbc/execute! (get-ds)
         [(str "SELECT * FROM " mr-table " WHERE id = ?") mr-id]) first)))

(defn- build-user-permissions-query
  [media-resource-id user-id perm-name mr-type]
  (-> (sql/select :*)
      (sql/from (user-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id]
        [:= :user_id user-id]
        [:= perm-name true])
      (sql-format)))

(defn- build-user-permission-get-query
  [media-resource-id mr-type user-id]
  (-> (sql/select :*)
      (sql/from (user-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id]
        [:= :user_id user-id])
      (sql-format)))

(defn- build-user-permission-list-query
  [media-resource-id mr-type]
  (-> (sql/select :*)
      (sql/from (user-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id])
      (sql-format)))

(defn- build-user-groups-query [user-id]
  (-> (sql/select :groups.*)
      (sql/from :groups)
      (sql/join :groups_users [:= :groups.id :groups_users.group_id])
      (sql/where [:= :groups_users.user_id user-id])
      (sql-format)))

(defn- query-user-groups [user-id]
  (->> (build-user-groups-query user-id)
    (jdbc/execute! (get-ds))))

(defn- build-group-permissions-query
  [media-resource-id group-ids perm-name mr-type]

  (println ">o> ??? build-group-permissions-query, group-ids=" group-ids)

  (let [
        result (-> (sql/select :*)
                   (sql/from (group-table mr-type))
                   (sql/where [:= (resource-key mr-type) media-resource-id]
                     [:in :group_id group-ids]              ;;broken?
                     [:= perm-name true])
                   (sql-format))

        p (println ">o> ??? build-group-permissions-query, result=" result)
        ])

  (-> (sql/select :*)
      (sql/from (group-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id]
        [:in :group_id group-ids]                           ;;broken?
        [:= perm-name true])
      (sql-format))

  )

(defn- build-group-permission-get-query
  [media-resource-id mr-type group-id]
  (-> (sql/select :*)
      (sql/from (group-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id]
        [:= :group_id group-id])
      (sql-format)))

(defn- build-group-permission-list-query
  [media-resource-id mr-type]
  (-> (sql/select :*)
      (sql/from (group-table mr-type))
      (sql/where [:= (resource-key mr-type) media-resource-id])
      (sql-format)))

; ============================================================

(defn- delegation-ids [user_id]
  (let [query {:union [(-> (sql/select :delegation_id)
                           (sql/from :delegations_groups)
                           (sql/where [:in :delegations_groups.group_id (->
                                                                         (sql/select :group_id)
                                                                         (sql/from :groups_users)
                                                                         (sql/where [:= :groups_users.user_id user_id]))]))
                       (-> (sql/select :delegation_id)
                           (sql/from :delegations_users)
                           (sql/where [:= :delegations_users.user_id user_id]))]}]
    (map #(:delegation_id %) (jdbc/execute! (get-ds) (sql-format query)))))

;(defn- query-api-client-permissions
;  [resource api-client-id perm-name mr-type]
;  (->> (build-api-client-permissions-query
;         (:id resource) api-client-id perm-name mr-type)
;       (jdbc/query (rdbms/get-ds))))

;(defn query-list-api-client-permissions
;  [resource mr-type]
;  (->> (build-api-client-permission-list-query
;        (:id resource) mr-type)
;       (jdbc/query (rdbms/get-ds))))

; TODO try catch logwrite
(defn update-resource-permissions
  [resource perm-data]
  (let [
        mr-id (-> resource :id)
        tname (case (:type resource)
                "MediaEntry" :media_entries
                "Collection" :collections)
        ;whcl (-> (sql/where [:= :id mr-id])
        ;         (sql-format)
        ;         (update-in [0] #(clojure.string/replace % "WHERE" "")))

        update-stmt (-> (sql/update tname)
                        (sql/set (convert-map-if-exist perm-data))
                        ;(sql/where (sql-format whcl))
                        (sql/where [:= :id (to-uuid mr-id)])
                        sql-format)
        upd-result (jdbc/execute-one! (get-ds) update-stmt)]

    (logging/info "update resource permissions"
      "\ntable\n" tname
      ;"\nwhcl\n" whcl
      "\nperm-data\n" perm-data)

    upd-result))

;(jdbc/update! (et-ds) tname perm-data whcl)))

(defn- query-user-permissions
  [resource user-id perm-name mr-type]


  (println ">o> user-perm / resource=" resource)

  (->> (build-user-permissions-query
         (:media_entry_id resource) user-id perm-name mr-type)
    (jdbc/execute! (get-ds))))

(defn query-list-user-permissions
  [resource mr-type]
  (->> (build-user-permission-list-query
         (:id resource) mr-type)
    (jdbc/execute! (get-ds))))

(defn query-get-user-permission
  [resource mr-type user-id]
  (jdbc/execute-one! (get-ds)
    (build-user-permission-get-query
      (:id resource) mr-type user-id)))

;; TODO
(defn- sql-cls-resource-and
  [mr-type mr-id and-key and-id]
  (-> (sql/where [:= (resource-key mr-type) mr-id]
        [:= and-key and-id])
      (sql-format)
      (update-in [0] #(clojure.string/replace % "WHERE" ""))))

(defn- sql-cls-resource-and-new
  [stmt mr-type mr-id and-key and-id]
  (-> stmt
      (sql/where [:= (resource-key mr-type) mr-id]
        [:= and-key and-id])
      )
  )


;TODO logwrite
(defn create-user-permissions
  [resource mr-type user-id data]
  ;(try
  ;  (catcher/with-logging {}
  (let [mr-id (:id resource)
        tname (user-table mr-type)
        insdata (assoc data :user_id user-id (resource-key mr-type) mr-id)

        insert-stmt (-> (sql/insert-into tname)
                        (sql/values [insdata])
                        (sql/returning :*)
                        sql-format)
        ins-result (jdbc/execute-one! (get-ds) insert-stmt)]

    ;ins-result (jdbc/insert! (rdbms/get-ds) tname insdata)]

    (logging/info "create-user-permissions" mr-id mr-type user-id tname insdata)
    ;(if-let [result (::jdbc/update-count ins-result)]
    (if-let [result ins-result]
      result
      nil)))
; (catch Exception ex
;   (logging/error "Could not create resource user permissions." (ex-message ex)))))

; TODO logwrite
(defn delete-user-permissions
  [resource mr-type user-id]
  ;(try
  ;  (catcher/with-logging {}
  (let [mr-id (:id resource)
        tname (user-table mr-type)
        ;delquery (sql-cls-resource-and mr-type mr-id :user_id user-id)

        ;delresult (jdbc/delete! (rdbms/get-ds) tname delquery)]

        delete-stmt (-> (sql/delete-from tname)
                        ;(sql/where (sql-format delquery))
                        (sql-cls-resource-and-new mr-type mr-id :user_id user-id)
                        sql-format)
        delresult (jdbc/execute-one! (get-ds) delete-stmt)]

    (logging/info "delete-user-permissions: " mr-id user-id delresult)
    (if (= 1 (::jdbc/update-count delresult))
      true
      false)))
;(catch Exception ex
;  ((logging/error "Could not delete resource user permissions." (ex-message ex))
;     false))))

; TODO logwrite
(defn update-user-permissions
  [resource mr-type user-id perm-name perm-val]

  (let [mr-id (:id resource)
        tname (user-table mr-type)
        ;whcl (sql-cls-resource-and mr-type mr-id :user_id user-id)
        perm-data {(keyword perm-name) perm-val}

        ;result (jdbc/update! (rdbms/get-ds) tname perm-data whcl)]

        update-stmt (-> (sql/update tname)

                        ;(sql/set perm-data)
                        (sql/set perm-data)

                        ;(sql/where (sql-format whcl))
                        (sql-cls-resource-and-new mr-type mr-id :user_id user-id)
                        sql-format)
        result (jdbc/execute-one! (get-ds) update-stmt)]

    (logging/info "update user permissions"
      "\ntable\n" tname
      ;"\nwhcl\n" whcl
      "\nperm-data\n" perm-data
      "\nresult:\n" result)
    result))

(defn- query-group-permissions
  [resource user-id perm-name mr-type]

  ;(let [
  ;
  ;
  ;      user-groups (seq (query-user-groups user-id))
  ;      p (println ">o> query-group-permissions, user-groups=" user-groups)
  ;
  ;      query (build-group-permissions-query (:id resource) (map :id user-groups) perm-name mr-type)
  ;
  ;      p (println ">o> query-group-permissions, query=" query)
  ;
  ;      result (jdbc/execute! (get-ds) query)
  ;
  ;      p (println ">o> query-group-permissions, result=" result)
  ;      ])

        (println ">o> resource=" resource)

  (if-let [user-groups (seq (query-user-groups user-id))]
    (->> (build-group-permissions-query
           (:media_entry_id resource) (map :id user-groups) perm-name mr-type)
           ;(:id resource) (map :id user-groups) perm-name mr-type)
      (jdbc/execute! (get-ds)))))

(defn query-get-group-permission
  [resource mr-type group-id]
  (first (jdbc/execute! (get-ds)
           (build-group-permission-get-query
             (:id resource) mr-type group-id))))

(defn query-list-group-permissions
  [resource mr-type]

  (println ">o> query-list-group-permissions" resource)
  (println ">o> query-list-group-permissions" mr-type)

  (->> (build-group-permission-list-query
         (:id resource) mr-type)
    (jdbc/execute! (get-ds))))

(defn create-group-permissions
  [resource mr-type group-id data]
  ;(try
  ;(catcher/with-logging {}
  (let [mr-id (:id resource)
        tname (group-table mr-type)
        insdata (assoc data :group_id group-id (resource-key mr-type) mr-id)

        ;insresult (jdbc/insert! (rdbms/get-ds) tname insdata)]

        insert-stmt (-> (sql/insert-into tname)
                        (sql/values [(convert-map-if-exist insdata)])
                        (sql/returning :*)
                        sql-format)
        insresult (jdbc/execute-one! (get-ds) insert-stmt)
        p (println ">o> insresult=" insresult)
        ]

    (logging/info "create-group-permissions" mr-id mr-type group-id tname insdata)
    (if-let [result insresult]
      result
      nil)))
;(catch Exception ex
;  (logging/error "ERROR: Could not create resource group permissions." (ex-message ex)))))

(defn delete-group-permissions
  [resource mr-type group-id]
  ;(try
  ;  (catcher/with-logging {}
  (let [mr-id (:id resource)
        tname (group-table mr-type)
        ;delquery (sql-cls-resource-and mr-type mr-id :group_id group-id)

        ;delresult (jdbc/delete! (rdbms/get-ds) tname delquery)]

        delete-stmt (-> (sql/delete-from tname)
                        ;(sql/where (sql-format delquery))
                        (sql-cls-resource-and-new mr-type mr-id :group_id group-id)

                        sql-format)
        delresult (jdbc/execute-one! (get-ds) delete-stmt)]

    (logging/info "delete-group-permissions: " mr-id group-id delresult)
    (if (= 1 (::jdbc/update-count delresult))
      true
      false)))
;  (catch Exception ex
;    ((logging/error "ERROR: Could not delete resource group permissions." (ex-message ex))
;     false))))

; TODO logwrite
(defn update-group-permissions
  [resource mr-type group-id perm-name perm-val]
  (let [mr-id (:id resource)
        tname (group-table mr-type)
        ;whcl (sql-cls-resource-and mr-type mr-id :group_id group-id)
        perm-data {(keyword perm-name) perm-val}

        ;result (jdbc/update! (rdbms/get-ds) tname perm-data whcl)]

        update-stmt (-> (sql/update tname)
                        (sql/set perm-data)
                        ;(sql/where (sql-format whcl))
                        ;(sql/where (sql-format whcl))
                        (sql-cls-resource-and-new mr-type mr-id :group_id group-id)
                        sql-format)
        result (jdbc/execute-one! (get-ds) update-stmt)]

    (logging/info "update group permissions"
      "\ntable\n" tname
      ;"\nwhcl\n" whcl
      "\nperm-data\n" perm-data
      "\nresult\n" result)
    result))

(defn permission-by-auth-entity? [resource auth-entity perm-name mr-type]
  (or (perm-name resource)
    (let [auth-entity-id (:id auth-entity)


          p (println ">o> ================================")
          p (println ">o> User0, (=auth-entity-id.." (= auth-entity-id (:responsible_user_id resource)))
          p (println ">o> User1, some=" (some #(= (:responsible_delegation_id resource) %) (delegation-ids auth-entity-id)))
          p (println ">o> User2, query-user-permissions=" (seq (query-user-permissions resource
                                                                 auth-entity-id
                                                                 perm-name mr-type)))

          p (println ">o> User3, query-group-permissions=" (seq (query-group-permissions resource
                                                                  auth-entity-id
                                                                  perm-name mr-type)))

          ]
      (-> (case (:type auth-entity)
            "User" (or (= auth-entity-id (:responsible_user_id resource))
                     (some #(= (:responsible_delegation_id resource) %) (delegation-ids auth-entity-id))
                     (seq (query-user-permissions resource
                            auth-entity-id
                            perm-name mr-type))
                     (seq (query-group-permissions resource
                            auth-entity-id
                            perm-name mr-type)))
            ;"ApiClient" (seq (query-api-client-permissions resource
            ;                                               auth-entity-id
            ;                                               perm-name mr-type))
            )
          boolean))))

(defn edit-permissions-by-auth-entity? [resource auth-entity mr-type]
  (let [auth-entity-id (:id auth-entity)]
    (-> (case (:type auth-entity)
          "User" (or (= auth-entity-id (:responsible_user_id resource))
                   (some #(= (:responsible_delegation_id resource) %) (delegation-ids auth-entity-id))
                   (seq (query-user-permissions resource
                          auth-entity-id
                          :edit_permissions
                          mr-type))))
        boolean)))

(defn viewable-by-auth-entity? [resource auth-entity mr-type]
  (permission-by-auth-entity? resource
    auth-entity
    :get_metadata_and_previews
    mr-type))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
