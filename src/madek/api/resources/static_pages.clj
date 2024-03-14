(ns madek.api.resources.static-pages
  (:require

   ;[clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   ;[madek.api.utils.rdbms :as rdbms :refer [get-ds]]

   [logbug.catcher :as catcher]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.shared :as sd]

   [madek.api.utils.helper :refer [cast-to-hstore]]

   [madek.api.utils.helper :refer [cast-to-hstore convert-map-if-exist f replace-java-hashmaps t v]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [schema.core :as s]))

(defn handle_list-static_pages
  [req]
  (let [full-data (true? (-> req :parameters :query :full_data))
        qd (if (true? full-data) :static_pages.* :static_pages.id)
        db-result (sd/query-find-all :static_pages qd)]
    (sd/response_ok db-result)))

(defn handle_get-static_page
  [req]

  (println ">o> handle_get-static_page")

  (let [static_page (-> req :static_page)
        static_page (sd/transform_ml_map static_page)
        ]
    (sd/response_ok static_page)))

(defn handle_create-static_page [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            ;contents-json (sd/try-as-json (:contents data))
            ;contents-json (:contents data)

            ;p (println ">o> contents-json" contents-json)
            ;p (println ">o> contents-json.class" (class contents-json))
            ;
            ;ins-data (assoc data :contents contents-json)
            ;ins-data (cast-to-hstore ins-data)

            ins-data (cast-to-hstore data)

            sql-query (-> (sql/insert-into :static_pages)
                          (sql/values [ins-data])
                          (sql/returning :*)
                          sql-format)

            ins-res (jdbc/execute-one! (get-ds) sql-query)
            p (println ">o> ins-res1=" ins-res)

            ins-res (sd/transform_ml_map ins-res)
            p (println ">o> ins-res2=" ins-res)

            ]

        (logging/info "handle_create-static-page:"
          "\ninsert data:\n" ins-data
          "\nresult:\n " ins-res)

        (if ins-res
          (sd/response_ok ins-res)
          (sd/response_failed "Could not create static_page." 406))))
    (catch Exception e (sd/parsed_response_exception e))))

(defn handle_update-static_page [req]
  (try
    (catcher/with-logging {}
      (let [data (-> req :parameters :body)
            id (-> req :parameters :path :id)
            ;contents-json (sd/try-as-json (:contents data))
            ;dwid (assoc data :id id :contents contents-json)
            ;upd-query (sd/sql-update-clause "id" (str id))

            ;upd-result (jdbc/update! (rdbms/get-ds)
            ;                         :static_pages
            ;                         dwid upd-query)]


            p (println ">o> data=" data)

            dwid (cast-to-hstore data)
            p (println ">o> dwid=" dwid)

            sql-query (-> (sql/update :static_pages)
                          (sql/set dwid)

                          ;(sql/where upd-query)
                          (sql/where [:= :id id])

                          (sql/returning :*)
                          sql-format)


            p (println ">o> sql-query=" sql-query)

            upd-result (jdbc/execute-one! (get-ds) sql-query)
            p (println ">o> upd-result=" upd-result)
            upd-result (sd/transform_ml_map upd-result)
            ]

        (logging/info "handle_update-static_pages: "
          "\nid:\n" id
          "\nnew-data:\n" dwid
          "\nupd-result:" upd-result)

        ;(if (= 1 (::jdbc/update-count upd-result))
        (if upd-result
          ;(sd/response_ok (sd/query-eq-find-one :static_pages :id id))
          (sd/response_ok upd-result)
          (sd/response_failed "Could not update static_page." 406))))
    (catch Exception e (sd/parsed_response_exception e))))

(defn handle_delete-static_page [req]
  (try
    (catcher/with-logging {}
      (let [olddata (-> req :static_page)
            id (-> req :parameters :path :id)

            ;delresult (jdbc/delete! (rdbms/get-ds)
            ;                        :static_pages
            ;                        ["id = ?" id])]

            sql-query (-> (sql/delete-from :static_pages)
                          (sql/where [:= :id id])
                          (sql/returning :*)
                          sql-format)

            delresult (jdbc/execute-one! (get-ds) sql-query)
            delresult (sd/transform_ml_map delresult)

            ]

        (logging/info "handle_delete-static_page: "
          " id: " id
          " result: " delresult)

        ;(if (= 1 (::jdbc/update-count delresult))
        (if delresult
          (sd/response_ok delresult)
          (sd/response_failed "Could not delete static page." 422))))

    (catch Exception e (sd/response_exception e))))

(defn wwrap-find-static_page [param]
  (fn [handler]
    (fn [request] (sd/req-find-data request handler param
                    :static_pages :id
                    :static_page true))))

(def schema_create_static_page
  {
   :name s/Str
   :contents sd/schema_ml_list
   })

(def schema_update_static_page
  {
   (s/optional-key :name) s/Str
   (s/optional-key :contents) sd/schema_ml_list
   })

; TODO Inst coercion
(def schema_export_static_page
  {:id s/Uuid
   :name s/Str
   :contents sd/schema_ml_list
   :created_at s/Any                                        ; TODO as Inst
   :updated_at s/Any})

; TODO auth admin
; TODO response coercion
; TODO docu
; TODO tests
; TODO user and admin routes
(def admin-routes

  ["/static-pages"
   {:swagger {:tags ["admin/static-pages"] :security [{"auth" []}]}}

   ["/"
    {:post {:summary (sd/sum_adm (t "Create static_page."))
            :handler handle_create-static_page
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema_create_static_page}
            :responses {200 {:body schema_export_static_page}

                        406 {:description "Not Acceptable."
                             :schema s/Str
                             :examples {"application/json" {:message "Could not create static_page."}}}

                        409 {:description "Conflict."
                             :schema s/Str
                             :examples {"application/json" {:message "Entry already exists"}}}

                        }}
     ; static_page list / query
     :get {:summary (sd/sum_adm (t "List static_pages."))
           :handler handle_list-static_pages
           :coercion reitit.coercion.schema/coercion
           :parameters {:query {(s/optional-key :full_data) s/Bool}}}}]
   ; edit static_page
   ["/:id"
    {:get {:summary (sd/sum_adm (t "Get static_pages by id."))
           :handler handle_get-static_page
           :middleware [(wwrap-find-static_page :id)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}}
           :responses {
                       200 {:body schema_export_static_page}

                       404 {:description "Not Found."
                            :schema s/Str
                            :examples {"application/json" {:message "No such entity in :static_pages as :id with <id>"}}}

                       }}

     :put {:summary (sd/sum_adm (t "Update static_pages with id."))
           :handler handle_update-static_page
           :middleware [(wwrap-find-static_page :id)]
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}
                        :body schema_update_static_page}
           :responses {200 {:body schema_export_static_page}
                       406 {:body s/Any}

                       404 {:description "Not Found."
                            :schema s/Str
                            :examples {"application/json" {:message "No such entity in :static_pages as :id with <id>"}}}

                            }}

     :delete {:summary (sd/sum_adm (t "Delete static_page by id."))
              :coercion reitit.coercion.schema/coercion
              :handler handle_delete-static_page
              :middleware [(wwrap-find-static_page :id)]
              :parameters {:path {:id s/Uuid}}
              :responses {
                          200 {:body schema_export_static_page}

                          404 {:description "Not Found."
                               :schema s/Str
                               :examples {"application/json" {:message "No such entity in :static_pages as :id with <id>"}}}

                          422 {:description "Unprocessable Entity."
                               :schema s/Str
                               :examples {"application/json" {:message "Could not delete static page."}}}

                          }}}]])