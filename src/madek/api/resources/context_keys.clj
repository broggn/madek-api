(ns madek.api.resources.context-keys
  (:require
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [clojure.tools.logging :as logging]

   [honey.sql :refer [format] :rename {format sql-format}]

   [honey.sql.helpers :as sql]

   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]

   [madek.api.pagination :as pagination]

   [madek.api.resources.shared :as sd]

   [madek.api.utils.auth :refer [wrap-authorize-admin!]]
   [madek.api.utils.helper :refer [cast-to-hstore]]

   [madek.api.utils.helper :refer [convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [madek.api.utils.helper :refer [t]]
   ;; all needed imports
   [next.jdbc :as jdbc]
   [pghstore-clj.core]
   [reitit.coercion.schema]

   [reitit.swagger :as swagger]
   [schema-tools.swagger.core :as swagger2]
   [schema.core :as s]

   [taoensso.timbre :refer [info warn error spy]]
   [taoensso.timbre :refer [spy]]))

(defn context_key_transform_ml [context_key]
  (assoc context_key
         :hints (sd/transform_ml (:hints context_key))
         :labels (sd/transform_ml (:labels context_key))
         :descriptions (sd/transform_ml (:descriptions context_key))
         :documentation_urls (sd/transform_ml (:documentation_urls context_key))))

(defn handle_adm-list-context_keys
  [req]
  (let [req-query (-> req :parameters :query)
        db-query (-> (sql/select :*)
                     (sql/from :context_keys)

                     (sd/build-query-param req-query :id)
                     (sd/build-query-param req-query :context_id)
                     (sd/build-query-param req-query :meta_key_id)
                     (sd/build-query-param req-query :is_required)

                     (sd/build-query-created-or-updated-after req-query :changed_after)
                     (sd/build-query-ts-after req-query :created_after "created_at")
                     (sd/build-query-ts-after req-query :updated_after "updated_at")

                     (pagination/add-offset-for-honeysql req-query)
                     sql-format)
        db-result (jdbc/execute! (get-ds) db-query)
        tf (map context_key_transform_ml db-result)]

    ;(logging/info "handle_adm-list-context_keys" "\ndb-query\n" db-query)
    (sd/response_ok tf)))

(defn handle_usr-list-context_keys
  [req]
  (let [req-query (-> req :parameters :query)
        db-query (-> (sql/select :id :context_id :meta_key_id
                                 :is_required :position :length_min :length_max
                                 :labels :hints :descriptions :documentation_urls)
                     (sql/from :context_keys)
                     (sd/build-query-param req-query :id)
                     (sd/build-query-param req-query :context_id)
                     (sd/build-query-param req-query :meta_key_id)
                     (sd/build-query-param req-query :is_required)
                     sql-format)
        db-result (jdbc/execute! (get-ds) db-query)
        tf (map context_key_transform_ml db-result)]

    ;(logging/info "handle_usr-list-context_keys" "\ndb-query\n" db-query)
    (sd/response_ok tf)))

(defn handle_adm-get-context_key
  [req]
  (let [result (-> req :context_key context_key_transform_ml)]
    ;(logging/info "handle_get-context_key: result: " result)
    (sd/response_ok result)))

(defn handle_usr-get-context_key
  [req]
  (let [context_key (-> req :context_key context_key_transform_ml)
        result (dissoc context_key :admin_comment :updated_at :created_at)]
    ;(logging/info "handle_usr-get-context_key" "\nbefore\n" context_key "\nresult\n" result)
    (sd/response_ok result)))

(comment

  (let [x (use '[pghstore-clj.core])

        data_row {:hints {:de "labelde99", :en "labelen88"} :context_id "agree-strikebreaker"
                  :meta_key_id "madek_core:title"}

        p (println "res-1" data_row)
        data_row (cast-to-hstore data_row)

        p (println "res-2" data_row)
        sql (-> (sql/insert-into :context_keys)

                (sql/values [data_row])

                (sql/returning :*)
                sql-format)

        ;;;;; =================================================================================================

        ;hints_casted (to-hstore {:de "labelde", :en "labelen"})
        ;p (println "res-2" hints_casted)
        ;sql (-> (sql/insert-into :context_keys)
        ;
        ;          (sql/values [{:hints hints_casted, :context_id "agree-strikebreaker", :meta_key_id "madek_core:title"}])
        ;
        ;          (sql/returning :*)
        ;          sql-format)

        p (println "res-1" sql)

        res (jdbc/execute! (get-ds) sql)]

    res))

(defn convert-hints [data]
  (let [cast-keys [:hints :descriptions]
        process-hstore-cast (fn [data key]

                              (if (contains? data key)
                                (let [val (get data key)
                                      hstore-val (when val
                                                   (str [:cast
                                                         (clojure.string/join ", "
                                                                              (map (fn [[k v]] (str (name k) " => \"" v "\""))
                                                                                   val))
                                                         :hstore]))]
                                  (assoc data key hstore-val))
                                data))]

    (reduce process-hstore-cast data cast-keys)))

(defn map-to-hstore [m]
  (clojure.string/join ", " (map (fn [[k v]] (str "\"" k "\" => \"" (clojure.string/replace v "\"" "\\\"") "\"")) m)))

(comment

  (let [;;data {:hints {:de "labelde", :en "labelen"}}
        ;;data {:hints {:de "labelde"}}
        ;;data {:hints [:cast "de => \"labelde\"" :hstore]}
        ;
        ;data {:hints {:de "labelde", :en "labelen"}}
        ;data {:hints {:de "labelde"}}
        ;;data {:hints [:cast "de => \"labelde\", en => \"labelen\"" :hstore]}
        ;
        ;data (convert-hints data)
        ;
        ;p (println ">o> res" data)

        data {:hints [:cast "de => \"labelde\"" :hstore]}
        ;data {:hints [:cast [:raw "de => \"labelde\""] :hstore]}
        data {:hints "'de => \"labelde\"'"}
        data {:hints "de => 'abc'"}
        ;data {:hints [:cast {:de "hstore"} :hstore]}

        data {:key1 "value1", :key2 "value2"}
        ;data {:meta_key_id "abc:1" :term "sed-dev" :external_uris [:cast (map-to-hstore data) :hstore]}
        ;data {:meta_key_id "abc:1" :term "sed-dev" :external_uris "{http://geonames.org/countries/DZ/}"}
        ;data {:meta_key_id "abc:1" :term "sed-dev" :external_uris (vector "{http://geonames.org/countries/DZ/}")}
        ;data {:meta_key_id "abc:1" :term "sed-dev" :external_uris [:cast "{http://geonames.org/countries/DZ/}" :char]}
        data {:meta_key_id "vid:vid_schwerpunkt" :term "sed-dev3" :external_uris [:raw "'{http://geonames.org/countries/DZ/}'"]}

        query (-> (sql/insert-into :keywords)
                  ;(sql/values [(cast-to-hstore data)])
                  (sql/values [data])
                  (sql/returning :*)
                  sql-format
                  spy)
        res (jdbc/execute-one! (get-ds) query)

        p (println "res-1" query)
        p (println "res-2" res)]))

(defn handle_create-context_keys
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            sql (-> (sql/insert-into :context_keys)
                    (sql/values [(spy (cast-to-hstore data))])
                    (sql/returning :*)
                    sql-format
                    spy)
            ins-res (jdbc/execute-one! (get-ds) sql)]

        (if-let [result ins-res]
          (sd/response_ok (context_key_transform_ml result))
          (sd/response_failed "Could not create context_key." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_update-context_keys
  [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            id (to-uuid id)
            dwid (spy (assoc data :id id))

            fir (-> (sql/update :context_keys)
                    (sql/set (cast-to-hstore dwid))
                    (sql/where [:= :id id])
                    sql-format)
            upd-result (jdbc/execute-one! (get-ds) (spy fir))]

        (sd/logwrite req (str "handle_update-context_keys: " id "\nnew-data\n" dwid "\nupd-result: " upd-result))

        (if (= 1 (::jdbc/update-count upd-result))
          (sd/response_ok (context_key_transform_ml (sd/query-eq-find-one :context_keys :id id)))
          (sd/response_failed "Could not update context_key." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_delete-context_key
  [req]
  (try
    (catcher/with-logging {}
      (let [context_key (-> req :context_key)
            id (-> req :context_key :id)
            sql (-> (sql/delete-from :context_keys)
                    (sql/where [:= :id id])
                    sql-format)
            del-result (jdbc/execute-one! (get-ds) sql)]

        (sd/logwrite req (str "handle_delete-context_key: " id " result: " del-result))
        (if (= 1 (::jdbc/update-count del-result))
          (sd/response_ok (context_key_transform_ml context_key))
          (logging/error "Could not delete context_key: " id))))
    (catch Exception ex (sd/response_exception ex))))

(defn wwrap-find-context_key [param colname send404]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler
                                    param
                                    :context_keys colname
                                    :context_key send404))))

(def schema_import_context_keys
  {;:id s/Str
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   (s/optional-key :length_max) (s/maybe s/Int)
   (s/optional-key :length_min) (s/maybe s/Int)
   :position s/Int
   (s/optional-key :admin_comment) (s/maybe s/Str)
   ; hstore
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)})

(def schema_update_context_keys
  {;(s/optional-key :id) s/Str
   ;:context_id s/Str
   ;(s/optional-key :meta_key_id) s/Str
   (s/optional-key :is_required) s/Bool
   (s/optional-key :length_max) (s/maybe s/Int)
   (s/optional-key :length_min) (s/maybe s/Int)
   (s/optional-key :position) s/Int
   (s/optional-key :admin_comment) (s/maybe s/Str)
   (s/optional-key :labels) (s/maybe sd/schema_ml_list)
   (s/optional-key :descriptions) (s/maybe sd/schema_ml_list)
   (s/optional-key :hints) (s/maybe sd/schema_ml_list)
   (s/optional-key :documentation_urls) (s/maybe sd/schema_ml_list)})

(def schema_export_context_key
  {:id s/Uuid
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   :length_max (s/maybe s/Int)
   :length_min (s/maybe s/Int)
   :position s/Int

   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)
   :hints (s/maybe sd/schema_ml_list)

   :documentation_urls (s/maybe sd/schema_ml_list)})

(def schema_export_context_key_admin
  {:id s/Uuid
   :context_id s/Str
   :meta_key_id s/Str
   :is_required s/Bool
   :length_max (s/maybe s/Int)
   :length_min (s/maybe s/Int)
   :position s/Int

   :labels (s/maybe sd/schema_ml_list)
   :descriptions (s/maybe sd/schema_ml_list)
   :hints (s/maybe sd/schema_ml_list)

   :documentation_urls (s/maybe sd/schema_ml_list)

   :admin_comment (s/maybe s/Str)
   :updated_at s/Any
   :created_at s/Any})

; TODO docu
; TODO tests
(def admin-routes
  ["/context-keys"
   {:swagger {:tags ["admin/context-keys"] :security [{"auth" []}]}}
   ["/"
    {:post
     {:summary (sd/sum_adm (t "Post context_key by id."))
      :swagger {:security [{"auth" []}]}
      :handler handle_create-context_keys
      :middleware [wrap-authorize-admin!]
      :content-type "application/json"
      :accept "application/json"
      :coercion reitit.coercion.schema/coercion
      :parameters {:body schema_import_context_keys}
      :responses {200 {:body schema_export_context_key_admin}
                  406 {:body s/Any}}}

     ; context_key list / query
     :get
     {:summary (sd/sum_adm (t "Query context_keys."))
      :handler handle_adm-list-context_keys
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :changed_after) s/Inst
                           (s/optional-key :created_after) s/Inst
                           (s/optional-key :updated_after) s/Inst
                           (s/optional-key :page) s/Int
                           (s/optional-key :count) s/Int
                           (s/optional-key :id) s/Uuid
                           (s/optional-key :context_id) s/Str
                           (s/optional-key :meta_key_id) s/Str
                           (s/optional-key :is_required) s/Bool}}
      :responses {200 {:body [schema_export_context_key_admin]}
                  406 {:body s/Any}}}}]
   ; edit context_key
   ["/:id"
    {:get
     {:summary (sd/sum_adm (t "Get context_key by id."))
      :handler handle_adm-get-context_key
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :put
     {:summary (sd/sum_adm (t "Update context_keys with id."))
      :handler handle_update-context_keys
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}
                   :body schema_update_context_keys}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm_todo (t "Delete context_key by id."))
      :coercion reitit.coercion.schema/coercion
      :handler handle_delete-context_key
      :middleware [wrap-authorize-admin!
                   (wwrap-find-context_key :id :id true)]
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export_context_key_admin}
                  404 {:body s/Any}
                  406 {:body s/Any}}}}]])

; TODO docu
(def user-routes
  ["/context-keys"
   {:swagger {:tags ["context-keys"]}}
   ["/"
    {:get
     {:summary (sd/sum_pub (t "Query / List context_keys."))
      :handler handle_usr-list-context_keys
      :coercion reitit.coercion.schema/coercion
      :parameters {:query {(s/optional-key :id) s/Uuid
                           (s/optional-key :context_id) s/Str
                           (s/optional-key :meta_key_id) s/Str
                           (s/optional-key :is_required) s/Bool}}
      :responses {200 {:body [schema_export_context_key]}
                  406 {:body s/Any}}}}]

   ["/:id"
    {:get
     {:summary (sd/sum_pub (t "Get context_key by id."))
      :handler handle_usr-get-context_key
      :middleware [(wwrap-find-context_key :id :id true)]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export_context_key}

                  400 {:message "Bad request"
                       :body {:schema {:id s/Str :Keyword s/Str}
                              :errors {:id s/Str}
                              :type s/Str
                              :coercion s/Str
                              :value {:id s/Str}
                              :in [s/Str]}}

                  404 {:message "Not found"
                       :body s/Any}}}}]])