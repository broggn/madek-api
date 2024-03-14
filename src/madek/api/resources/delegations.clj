(ns madek.api.resources.delegations
  (:require
   [clojure.java.jdbc :as jdbco]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]

   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   ;[madek.api.utils.sql :as sqlo]

   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist t f]]


   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]

   [reitit.coercion.schema]
   [schema.core :as s]))

(defn handle_list-delegations
  [req]
  (let [full-data (= "true" (get (-> req :query-params) "full-data"))
        qd (if (true? full-data) :* :delegations.id)
        db-result (sd/query-find-all :delegations qd)]
    ;(logging/info "handle_list-delegation" "\nqd\n" qd "\nresult\n" db-result)
    (sd/response_ok db-result)))

(defn handle_get-delegation
  [req]
  (let [delegation (-> req :delegation)]
    (logging/info "handle_get-delegation" delegation)
    ; TODO hide some fields
    (sd/response_ok delegation)))

(defn handle_create-delegations
  [req]

  (let [data (-> req :parameters :body)
        insert-map {:table :delegations :values data}
        sql-query (-> (sql/insert-into :delegations) (sql/values [data]) sql-format)
        ins-res (jdbc/execute! (get-ds) sql-query)]
    (if ins-res
      (sd/response_ok (first ins-res))
      (sd/response_failed "Could not create delegation." 406))))

;(let [data (-> req :parameters :body)
;      ; or TODO data with id
;      ]
;      ; create delegation entry
;  (if-let [ins_res (jdbc/insert! (get-ds) :delegations data)]
;      ; TODO clean result
;    (sd/response_ok (first ins_res))
;    (sd/response_failed "Could not create delegation." 406))))

(defn handle_update-delegations
  [req]
  (let [data (-> req :parameters :body)
        id (-> req :parameters :path :id)
        dwid (assoc data :id id)
        old-data (-> req :delegation)
        upd-query (sd/sql-update-clause "id" (str id))
        ; or TODO data with id

        sql-query (-> (sql/update :delegations) (sql/set dwid) (sql/where [:= :id id]) sql-format)]

    ; create delegation entry
    (logging/info "handle_update-delegations: " "\nid\n" id "\ndwid\n" dwid
      "\nold-data\n" old-data
      "\nupd-query\n" upd-query)
    (if-let [ins-res (first (jdbc/execute! (get-ds) sql-query))]
      (let [new-data (sd/query-eq-find-one :delegations :id id)]
        (logging/info "handle_update-delegations:" "\nnew-data\n" new-data)
        (sd/response_ok new-data))
      (sd/response_failed "Could not update delegation." 406))))

;(if-let [ins-res (jdbc/update! (get-ds) :delegations dwid upd-query)]
;    ; TODO clean result
;  ;(if (= 1 ins-res)
;  (let [new-data (sd/query-eq-find-one :delegations :id id)]
;    (logging/info "handle_update-delegations:" "\nnew-data\n" new-data)
;    (sd/response_ok new-data))
;   ; (sd/response_failed "Could not update delegation." 406)
;   ; )
;  (sd/response_failed "Could not update delegation." 406))))

(defn handle_delete-delegation
  [req]
  (try
    (catcher/with-logging {}

      (if-let [_ (sd/query-eq-find-one :delegation :id (-> req :delegation :id))]

        (let [delegation (-> req :delegation)
              id (-> req :delegation :id)
              sql-query (-> (sql/delete-from :delegations)
                            (sql/where [:= :id id])
                            (sql/returning :*)
                            sql-format)]

          (if (jdbc/execute-one! (get-ds) sql-query)
            (sd/response_ok delegation)
            (sd/response_failed "Could not delete delegation." 406)))
        (sd/response_not_found "No such delegation found.")))

    (catch Exception ex (sd/parsed_response_exception ex))))


(defn wwrap-find-delegation [param colname send404]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler param
                    :delegations colname :delegation send404))))

(def schema_import_delegations
  {;:id s/Str
   :name s/Str
   :description s/Str
   :admin_comment (s/maybe s/Str)})

(def schema_update_delegations
  {;(s/optional-key :id) s/Str
   (s/optional-key :name) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :admin_comment) (s/maybe s/Str)})

(def schema_get_delegations
  {:id s/Uuid
   (s/optional-key :name) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :admin_comment) (s/maybe s/Str)})

; TODO Inst coercion
(def schema_export_delegations
  {:id s/Uuid
   :name s/Str
   :description s/Str
   :admin_comment (s/maybe s/Str)})

; TODO more checks
; TODO response coercion
; TODO docu
; TODO tests
(def ring-routes

  ["/delegations"
   {:swagger {:tags ["delegation"] :security [{"auth" []}]}}
   ["/"
    {:post {:summary (sd/sum_adm_todo (t "Create delegations."))
            ; TODO labels and descriptions
            :handler handle_create-delegations
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema_import_delegations}
            :responses {200 {:body schema_export_delegations}
                        406 {:body s/Any}}}
     ; delegation list / query
     :get {:summary (sd/sum_adm (t "List delegations."))
           :handler handle_list-delegations
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces "application/json"
                     :parameters [{:name "full-data"
                                   :in "query"
                                   :description "Request full data."
                                   :required true
                                   :value false
                                   :default false
                                   :type "boolean"
                                   ;:type s/Bool
                                   ;:pattern "^[1-9][0-9]*$"
                                   }]
                     }

           :responses {200 {:body [schema_get_delegations]}
                       }}}]

   ; edit delegation
   ["/:id"
    {:get {:summary (sd/sum_adm (t "Get delegations by id."))
           :handler handle_get-delegation
           :middleware [(wwrap-find-delegation :id :id true)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}}
           :responses {
                       200 {:body schema_export_delegations}
                       404 {:description "Not Found."
                            :schema s/Str
                            :examples {"application/json" {:message "No such entity in :delegations as :id with <id>"}}}
                       }}

     :put {:summary (sd/sum_adm "Update delegations with id.")
           ; TODO labels and descriptions
           :handler handle_update-delegations
           :middleware [(wwrap-find-delegation :id :id true)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}
                        :body schema_update_delegations}
           :responses {200 {:body s/Any}                    ;schema_export_delegations}
                       406 {:body s/Any}}}

     :delete {:summary (sd/sum_adm_todo (t "Delete delegation by id."))
              :coercion reitit.coercion.schema/coercion
              :handler handle_delete-delegation
              :middleware [(wwrap-find-delegation :id :id true)]
              :parameters {:path {:id s/Uuid}}

              :responses {
                          200 {:body schema_export_delegations}
                          404 {:description "Not Found."
                               :schema s/Str
                               ;:examples {"application/json" {:message "No such entity in :delegations as :id with <id>"}}}
                               :examples {"application/json" {:message "No such delegation found"}}}

                          406 {:description "Not Acceptable."
                               :schema s/Str
                               :examples {"application/json" {:message "Could not delete delegation."}}}
                          }

              }}]])