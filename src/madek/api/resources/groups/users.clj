(ns madek.api.resources.groups.users
  (:require
   [clj-uuid]
   [clojure.tools.logging :as logging]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.pagination :as pagination]
   [madek.api.resources.groups.shared :as groups]

   [madek.api.utils.helper :refer [convert-groupid-userid]]

   ; all needed imports
   [madek.api.resources.shared :as sd]
   [madek.api.utils.helper :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [schema.core :as s]
   [taoensso.timbre :refer [spy]]))

;;; temporary users stuff ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sql-select
  ([] (sql-select {}))
  ([sql-map]
   (sql/select sql-map :*
     ;:users.id :users.email :users.institutional_id :users.login
     ;:users.created_at :users.updated_at
     ;:users.person_id
     )))

(defn sql-merge-user-where-id
  ([id] (sql-merge-user-where-id {} id))
  ([sql-map id]
   (if (instance? java.util.UUID id)
     ;(if (re-matches
     ;   #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
     ;   id)
     (sql/where sql-map [:or
                         [:= :users.id id]
                         [:= :users.institutional_id (str id)]
                         [:= :users.email (str id)]])
     (sql/where sql-map [:or
                         [:= :users.institutional_id (str id)]
                         [:= :users.email (str id)]]))))

(defn find-user-sql [some-id]
  (-> (sql-select)
      (sql-merge-user-where-id some-id)
      (sql/from :users)
      sql-format))

(defn find-user [some-id]
  (->> some-id find-user-sql
    (jdbc/execute-one! (get-ds))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-user-query [group-id user-id]
  (->                                                       ;(users/sql-select)
   (sql/select {} :users.id :users.institutional_id :users.email :users.person_id)
   (sql/from :users)
   (sql/join :groups_users [:= :users.id :groups_users.user_id])
   (sql/join :groups [:= :groups.id :groups_users.group_id])
   (sql-merge-user-where-id user-id)
   (groups/sql-merge-where-id group-id)
   sql-format
   spy
   ))

(defn find-group-user [group-id user-id]
  (->> (group-user-query group-id user-id)
    (jdbc/execute-one! (get-ds))))

(defn get-group-user [group-id user-id]
  (if-let [user (find-group-user group-id user-id)]
    (sd/response_ok user)
    (sd/response_failed "No such group or user." 404)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;TODO test paging
(defn group-users-query [group-id request]
  (-> (sql/select :users.id :users.institutional_id :users.email :users.person_id)
      (sql/from :users)
      (sql/join :groups_users [:= :users.id :groups_users.user_id])
      (sql/join :groups [:= :groups.id :groups_users.group_id])
      (sql/order-by [:users.id :asc])
      (groups/sql-merge-where-id group-id)
      (pagination/add-offset-for-honeysql (-> request :parameters :query))
      sql-format))

(defn group-users [group-id request]
  (jdbc/execute! (get-ds)
    (group-users-query group-id request)))

(defn get-group-users [group-id request]
  (sd/response_ok {:users (group-users group-id request)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-user [group-id user-id]
  (logging/info "add-user" group-id ":" user-id)

  (println ">o> 0find-group-user=" (find-group-user group-id user-id))
  (println ">o> 0group" group-id)
  (println ">o> 0group.cl" (class group-id))
  (println ">o> 0user" user-id)
  (println ">o> 0user.cl" (class user-id))

  (if-let [user (spy (find-group-user group-id user-id))]
    (sd/response_ok {:users (group-users group-id nil)})
    (let [group (groups/find-group group-id)
          user (find-user user-id)


          p (println ">o> 1group" group)
          p (println ">o> 1group.cl" (class group))
          p (println ">o> 1user" user)
          p (println ">o> 1user.cl" (class user))

          ]
      (if-not (and group user)
        (sd/response_not_found "No such user or group.")
        (do (jdbc/execute! (get-ds)
              (-> (sql/insert-into :groups_users)
                  (sql/values [{:group_id (:id group)
                                :user_id (:id user)}])
                  sql-format))
            (sd/response_ok {:users (group-users group-id nil)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [group-id user-id]
  (if-let [user (find-group-user group-id user-id)]
    (if-let [group (groups/find-group group-id)]
      (let [delok (jdbc/execute! (get-ds)
                    (-> (sql/delete-from :groups_users)
                        (sql/where [:= :group_id (:id group)] [:= :user_id (:id user)])
                        sql-format))]
        (sd/response_ok {:users (group-users group-id nil)}))
      (sd/response_not_found "No such group"))
    (sd/response_not_found "No such group or user.")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-group-users-ids [tx group-id]
  (println ">o> current-group-users-ids???" group-id)
  (println ">o> current-group-users-ids??? class" (class group-id))

  (let [
        res (jdbc/execute! tx (-> (sql/select-distinct :user_id)
                                 (sql/from :groups_users)
                                 ;(sql/where [:= :groups_users.group_id (to-uuid group-id)])
                                 (sql/where [:= :group_id group-id])
                                 sql-format
                                 spy
                                 ))
        new res

        p (println ">o> a res=" res)
        p (println ">o> a2 res=" (count res))

        ;res2 (map :users/id res)

        res3 (map :groups_users/group_id res )
        p (println ">o> b res=" res3)

        ;res2 (map :group_id res )
        ;p (println ">o> b? res=" res2)

        res (set res3)
        p (println ">o> c res=" res)


        res (set (map :groups_users/user_id new))
        p (println ">o> c res-new=" res)

        ]res)

  ;(spy (->> (jdbc/execute! tx (-> (sql/select-distinct :*)
  ;                                (sql/from :groups_users)
  ;                                ;(sql/where [:= :groups_users.group_id (to-uuid group-id)])
  ;                                (sql/where [:= :group_id group-id])
  ;                                sql-format
  ;                                spy
  ;                                ))
  ;       spy
  ;       (map :groups_users/group_id)
  ;       set))

  )

(defn target-group-users-query [users]

   (println ">o> ??? 1 target-group-users-query=" users)
   (println ">o> ??? 2 target-group-users-query=" (->> users
                                                     (map #(-> % :id to-uuid))))

   (println ">o> ??? 3 target-group-users-query=" (->> users
                                                     (map #(-> % :id to-uuid))
                                                     ;(map #(-> % :users.id to-uuid))
                                                     (filter identity)))

  (-> (sql/select :id)
      (sql/from :users)
      (sql/where                                            ;[:or
        [:in :users.id (->> users
                         (map #(-> % :id to-uuid))
                         ;(map #(-> % :users.id to-uuid))
                         (filter identity))])
      sql-format))

(defn target-group-users-ids [tx users]

  (let [

        p (println ">o> f users=" users)

        res (jdbc/execute! tx (target-group-users-query users))
        p (println ">o> g res" res)


        p (println ">o> abc first" (first res))
        p (println ">o> abc first" (keys (first res)))


        res2 (map :users/id res)
        p (println ">o> h res" res2)

        res (set res2)
        p (println ">o> res.count" (count res2))

        ]res)


  ;(->> (jdbc/execute! tx (target-group-users-query users))
  ;  (map :users/id)
  ;  set)

  )

(defn update-delete-query [group-id ids]                    ;;here
  (println ">o> update-delete-query1 " group-id)
  (println ">o> update-delete-query1 " ids)
  (-> (sql/delete-from :groups_users)
      ;(sql/where [:= :groups_users.group_id (to-uuid group-id)])
      (sql/where [:= :groups_users.group_id group-id])
      (sql/where [:in :groups_users.user_id ids])
      sql-format))


;(defn update-delete-query [group-id ids]
;  (-> (sql/delete-from :groups_users)
;      (sql/merge-where [:= :groups_users.group_id group-id])
;      (sql/merge-where [:in :groups_users.user_id ids])
;      sql/format))

;(defn update-insert-query [group-id ids]
;  (println ">o> update-insert-query1 " group-id)
;  (println ">o> update-insert-query1 " ids)
;
;
;  (let [
;        ;; TODO: FIX THIS WITH PRIO!!
;
;        p (println ">o> ? group-id" group-id)
;        p (println ">o> ? group-id.cl" (class group-id))
;
;        values (->> ids (map (fn [id] [(to-uuid group-id) (to-uuid id)])))
;        p (println ">o> values=" values)
;
;        res (-> (sql/insert-into :groups_users)
;                (sql/columns :group_id :user_id)
;                (sql/values values)
;                ;(sql/upsert (-> (sql/on-conflict :group_id)
;                ;                (sql/do-update-set :user_id)))
;                sql-format)
;
;        ] res)
;
;  )

(defn update-insert-query [group-id ids]
  (-> (sql/insert-into :groups_users)
      (sql/columns :group_id :user_id)
      (sql/values (->> ids (map (fn [id] [group-id id]))))
      sql-format))

(defn update-group-users [group-id data]

  (println ">o> update-group-users1" group-id)
  (println ">o> update-group-users2" data)



  (jdbc/with-transaction [tx (get-ds)]
    (println ">o> update-group-users ==========================================")


    (let [current-group-users-ids (current-group-users-ids tx group-id)
          p (println ">o> 1 a current-group-users-ids" current-group-users-ids)
          p (println ">o>>>> 1 b current-group-users-ids" (count current-group-users-ids))
          target-group-users-ids (if (first (:users data))
                                   (target-group-users-ids tx (:users data))
                                   [])

          p (println ">o> 2 target-group-users-ids" target-group-users-ids)
          p (println ">o>>>> 2 target-group-users-ids" (count target-group-users-ids))
          p (println ">o> -----------------------")

          del-users (clojure.set/difference current-group-users-ids target-group-users-ids)
          p (println ">o> 3 del-users" (count del-users))
          p (println ">o> -----------------------")

          ins-users (clojure.set/difference target-group-users-ids current-group-users-ids)
          p (println ">o> 4 ins-users" (count ins-users))
          p (println ">o> -----------------------")

          del-query (update-delete-query group-id del-users)
          ins-query (update-insert-query group-id ins-users)]
      ;(logging/info "update-group-users" "\ncurr\n" current-group-users-ids "\ntarget\n" target-group-users-ids )
      ;(logging/info "update-group-users" "\ndel-u\n" del-users)
      ;(logging/info "update-group-users" "\nins-u\n" ins-users)
      ;(logging/info "update-group-users" "\ndel-q\n" del-query)
      ;(logging/info "update-group-users" "\nins-q\n" ins-query)



      (let [
            p (println ">o> 1del-query" del-users)
            p (println ">o> 2del-query" (first del-users))
            p (println ">o> 3del-query" (count del-users))
            p (println ">o> 4del-query" del-query)


            p (println ">o> 11del-query" ins-users)
            p (println ">o> 12del-query" (first ins-users))
            p (println ">o> 13del-query" (count ins-users))
            p (println ">o> 14del-query" ins-query)

            ])

      (when (first del-users)
        (do
          (println ">o> (first del-users)")
          (jdbc/execute!
            tx
            del-query)))

      (when (first ins-users)                               ;; TODO: causes error
        (do
          (println ">o> (first ins-users)")
          (jdbc/execute!
            tx
            ins-query))
        )

      (let [
            p (println ">o> 1del-query" del-users)
            p (println ">o> 2del-query" (first del-users))
            p (println ">o> 3del-query" (count del-users))
            p (println ">o> 4del-query" del-query)


            p (println ">o> 11del-query" ins-users)
            p (println ">o> 12del-query" (first ins-users))
            p (println ">o> 13del-query" (count ins-users))
            p (println ">o> 14del-query" ins-query)

            ;res (sd/response_ok {:users (jdbc/query tx
            ;                              (group-users-query group-id nil))})


            res (sd/response_ok {:users (jdbc/execute! tx
                                          (group-users-query group-id nil)
                                          jdbc/unqualified-snake-kebab-opts
                                          )})

            ]

        (println ">o> =============================================================")


        res


        ;(spy (sd/response_ok {:users (jdbc/execute! tx
        ;                               (group-users-query group-id nil)
        ;                               jdbc/unqualified-snake-kebab-opts
        ;                               )}))

        ))))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (def schema_export-group-user
    {:id s/Uuid
     :email (s/maybe s/Str)
     :institutional_id (s/maybe s/Str)
     :login (s/maybe s/Str)
     :created_at s/Any
     :updated_at s/Any
     :person_id s/Uuid})

  (def schema_export-group-user-simple
    {:id s/Uuid
     :email (s/maybe s/Str)
     :institutional_id (s/maybe s/Str)
     ;:login (s/maybe s/Str)
     ;:created_at s/Any
     ;:updated_at s/Any
     :person_id (s/maybe s/Uuid)})

  (def schema_update-group-user-list
    {:users
     [{(s/required-key :id) s/Uuid
       ;[{(s/optional-key :id) s/Uuid
       (s/optional-key :institutional_id) s/Uuid
       (s/optional-key :email) s/Str}]})

  (defn handle_get-group-user [req]
    (let [group-id (-> req :parameters :path :group-id)
          user-id (-> req :parameters :path :user-id)

          converted (convert-groupid-userid group-id user-id)


          _ (if (-> converted :is_userid_valid)
              (sd/response_failed "Invalid user-id." 400))

          group-id (-> converted :group-id)
          user-id (-> converted :user-id)

          ]



      (logging/info "handle_get-group-user" "\ngroup-id\n" group-id "\nuser-id\n" user-id)
      (get-group-user group-id user-id)))

  (defn handle_delete-group-user [req]
    (let [group-id (-> req :parameters :path :group-id)
          user-id (-> req :parameters :path :user-id)]
      (logging/info "handle_delete-group-user" "\ngroup-id\n" group-id "\nuser-id\n" user-id)
      (remove-user group-id user-id)))

  (defn handle_get-group-users [request]
    (let [id (-> request :parameters :path :group-id)]
      (get-group-users id request)))

  (defn handle_update-group-users [req]
    (let [id (-> req :parameters :path :group-id)
          data (-> req :parameters :body)
          ;data (-> req :parameters :body :users)

          p (println ">o> ??????????? handle_update-group-users")
          p (println ">o> id=" id)
          p (println ">o> data.count=" (count (:users data)))

          ]
      (logging/info "handle_update-group-users" "\nid\n" id "\ndata\n" data)
      (update-group-users id data)))                        ;; here

  (defn handle_add-group-user [req]
    (let [group-id (-> req :parameters :path :group-id)
          user-id (-> req :parameters :path :user-id)

          converted (convert-groupid-userid group-id user-id)

          p (println ">o> handle_add-group-user / converted=" converted)

          group-id (-> converted :group-id)
          user-id (-> converted :user-id)

          ]
      (add-user group-id user-id)))

  ;### Debug ####################################################################
  ;(debug/debug-ns *ns*)
