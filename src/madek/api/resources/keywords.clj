(ns madek.api.resources.keywords
  (:require
   [cheshire.core :as json]
   [honey.sql :refer [format] :rename {format sql-format}]
   ;[clojure.java.jdbc :as jdbc]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]

   [logbug.debug :as debug]

   ;[leihs.core.db :as db]
   ;[madek.api.utils.rdbms :refer [get-ds]]
   [madek.api.db.core :refer [get-ds]]
   ;[madek.api.db.core :refer [get-ds]]
   [madek.api.resources.keywords.keyword :as kw]

   [madek.api.resources.shared :as sd]
   ;; all needed imports
   [madek.api.utils.auth :refer [wrap-authorize-admin!]]

   [madek.api.utils.helper :refer [array-to-map map-to-array format-uris convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [madek.api.utils.helper :refer [t d]]

   [next.jdbc :as jdbc]
   [reitit.coercion.schema]

   [schema.core :as s]

   [taoensso.timbre :refer [info warn error spy]]))

;### swagger io schema ####################################################################

(def schema_create_keyword
  {:meta_key_id s/Str
   :term s/Str
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :position) (s/maybe s/Int)
   (s/optional-key :external_uris) [s/Str]
   (s/optional-key :rdf_class) s/Str})

(def schema_create_keyword2
  {:meta_key_id s/Str
   :term s/Str

   ;(s/optional-key :description) (s/maybe s/Str)
   ;(s/optional-key :position) (s/maybe s/Int)
   ;(s/optional-key :external_uris) [s/Str]
   ;(s/optional-key :rdf_class) s/Str
   })
(def schema_update_keyword
  {;id
   ;(s/optional-key :meta_key_id) s/Str
   (s/optional-key :term) s/Str
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :position) s/Int
   (s/optional-key :external_uris) [s/Str]
   (s/optional-key :rdf_class) s/Str})

(def schema_export_keyword_usr
  {:id s/Uuid
   :meta_key_id s/Str
   :term s/Str
   :description (s/maybe s/Str)
   :position (s/maybe s/Int)
   :external_uris [s/Any]
   :external_uri (s/maybe s/Str)
   :rdf_class s/Str})

(def schema_export_keyword_adm
  {:id s/Uuid
   :meta_key_id s/Str
   :term s/Str
   :description (s/maybe s/Str)
   :position (s/maybe s/Int)
   :external_uris [s/Any]
   :external_uri (s/maybe s/Str)
   :rdf_class s/Str
   :creator_id (s/maybe s/Uuid)
   :created_at s/Any
   :updated_at s/Any})

(def schema_export_keyword_adm2
  {;:id s/Uuid
   :meta_key_id s/Str
   :term s/Str
   ;:description (s/maybe s/Str)
   ;:position (s/maybe s/Int)
   ;:external_uris [s/Any]
   ;:external_uri (s/maybe s/Str)
   ;:rdf_class s/Str
   ;:creator_id (s/maybe s/Uuid)
   ;:created_at s/Any
   ;:updated_at s/Any
   })

(def schema_query_keyword
  {(s/optional-key :id) s/Uuid
   (s/optional-key :meta_key_id) s/Str
   (s/optional-key :term) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :rdf_class) s/Str
   (s/optional-key :page) s/Int
   (s/optional-key :count) s/Int})

(defn user-export-keyword [keyword]
  (->
   keyword
   ;(select-keys
   ; [:id :meta_key_id :term :description :external_uris :rdf_class
   ;  :created_at])
   (dissoc :creator_id :created_at :updated_at)
   (assoc ; support old (singular) version of field
    :external_uri (first (keyword :external_uris)))))

(defn adm-export-keyword [keyword]
  (->
   keyword
   (assoc ; support old (singular) version of field
    :external_uri (first (keyword :external_uris)))))

;### handlers get and query ####################################################################

(defn handle_adm-get-keyword
  [request]
  (let [keyword (-> request :keyword)]
    (sd/response_ok (adm-export-keyword keyword))))

(defn handle_usr-get-keyword
  [request]
  (let [keyword (-> request :keyword)]
    (sd/response_ok (user-export-keyword keyword))))

(defn handle_usr-query-keywords [request]
  (let [rq (-> request :parameters :query)
        db-result (kw/db-keywords-query rq)
        result (map user-export-keyword db-result)]
    (sd/response_ok {:keywords result})))

(defn handle_adm-query-keywords [request]
  (let [rq (-> request :parameters :query)
        db-result (kw/db-keywords-query rq)
        result (map adm-export-keyword db-result)]
    (sd/response_ok {:keywords result})))

;### handlers write ####################################################################

(defn handle_create-keyword [req]
  (try
    (catcher/with-logging {}
      (let [uid (-> req :authenticated-entity :id)

            ;;; TODO: remove this
            ;uid #uuid "11571ab5-293c-40d8-bd01-89597fdf3daf"

            data (-> req :parameters :body)
            dwid (assoc data :creator_id uid)

            ;; TODO / FIXME:  convert external_uris to hstore
            sql-query (-> (sql/insert-into :keywords)
                          (sql/values [(convert-map dwid)])
                          (sql/returning :*)
                          ;(sql/returning :meta_key_id :term)
                          sql-format
                          spy)

            ins-result (jdbc/execute-one! (get-ds) sql-query)
            p (println ">o> ins-result" ins-result)]

        (if-let [result ins-result]
          (sd/response_ok (adm-export-keyword result))
          (sd/response_failed "Could not create keyword" 406))))
    (catch Exception ex (sd/response_exception ex))))

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

(defn urls-to-custom-format [urls]
  (let [transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))

(comment

  (let [uris ["http://www.ige.ch", "http://www.example.com"]
        ;uris ["http://www.examp2le.com"]

        dwid {:meta_key_id "copyright:license"
              :term "aaa99-8s22292"

              ;:external_uris [:raw "'{test/me/now/78}'"]    ;;works
              ;:external_uris [:raw "'{test/me/now/78,test/me/now/99}'"]    ;;works
              :external_uris (urls-to-custom-format uris) ;;works
              }

        sql-query (-> (sql/insert-into :keywords)
                      (sql/values [dwid])
                      (sql/returning :*)
                      ;(sql-format :inline true)
                      sql-format
                      spy)
        ins-result (jdbc/execute-one! (get-ds) sql-query)
        p (println ">o> ins-result" ins-result)

        res (adm-export-keyword ins-result)]

    res))

(defn handle_update-keyword [req]
  (try
    (catcher/with-logging {}
      (let [id (-> req :parameters :path :id)
            data (-> req :parameters :body)
            ;; FIXME
            ;upd-res (jdbc/update!
            ;          (get-ds) :keywords data
            ;          (sd/sql-update-clause "id" id))]

            sql-query (-> (sql/update :keywords)
                          (sql/set (convert-map data))
                          (sql/where [:= :id id])
                          sql-format)
            upd-res (jdbc/execute-one! (get-ds) sql-query)]

        (if (= 1 (:next.jdbc/update-count upd-res))
          ;(sd/response_ok (adm-export-keyword (kw/db-keywords-get-one id)))
          (-> id kw/db-keywords-get-one
              adm-export-keyword
              sd/response_ok)
          (sd/response_failed "Could not update keyword." 406))))
    (catch Exception ex (sd/response_exception ex))))

(defn handle_delete-keyword [req]
  (try
    (catcher/with-logging {}
      (let [id (-> req :parameters :path :id)
            old-data (-> req :keyword)

            ;del-res (jdbc/delete!
            ;          (get-ds) :keywords
            ;          (sd/sql-update-clause "id" id))]

            sql-query (-> (sql/delete-from :keywords)
                          (sql/where [:= :id id])
                          sql-format)
            del-res (jdbc/execute-one! (get-ds) sql-query)]

        ; logwrite
        (if (= 1 (::jdbc/update-count del-res))
          (sd/response_ok (adm-export-keyword old-data))
          (sd/response_failed "Could not delete keyword." 406))))
    (catch Exception ex (sd/response_exception ex))))

;### routes ###################################################################

(defn wrap-find-keyword [handler]
  (fn [request] (sd/req-find-data request handler
                                  :id
                                  :keywords :id
                                  :keyword true)))

(s/defschema ItemQueryParams
  {:page (s/constrained s/Int #(>= % 1) "Must be a positive integer")
   :size2 (s/constrained s/Int #(>= % 1) "Must be a positive integer")})

;; FIXME: broken endpoint to test doc
(def query-routes
  ["/keywords"
   {:swagger {:tags ["keywords"]}}
   ["/"
    {:get
     {:summary (sd/sum_pub (d (t "Query / list keywords.")))
      :handler handle_usr-query-keywords
      :coercion reitit.coercion.schema/coercion
      ;:parameters {:query schema_query_keyword}

      ;:parameters {:query ItemQueryParams}

      :parameters {:query ItemQueryParams}

      :swagger {:parameters [{:name "page1"
                              :in "query"
                              :description "Page number, defaults to 1"
                              :required false
                              :value 1
                              :default 3
                              :defaults 2
                              ;:schema {:type "integer"
                              ;         :format "int32"
                              ;         :value 11
                              ;         :defaults 22
                              ;         :default 44}
                              }
                             {:name "size2"
                              :in "query"
                              :description "Number of items per page, defaults to 10"
                              :required false
                              :value 999
                              :schema {:type "integer"
                                       :format "int32"
                                       :default 10}}]}

      :responses {200 {:body {:keywords [schema_export_keyword_usr]}}

                  202 {:description "Successful response, list of items."
                       :schema {} ;; Define your response schema as needed
                       :examples {"application/json" {:message "Here are your items."
                                                      :page 1
                                                      :size 2
                                                      :items [{:id 1, :name "Item 1"}
                                                              {:id 2, :name "Item 2"}]}}}}}}]

   ["/:id"
    {:get
     {:summary (sd/sum_pub (t "Get keyword for id."))
      :handler handle_usr-get-keyword
      :middleware [wrap-find-keyword]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export_keyword_usr}
                  404 {:body s/Any}}
      :description "Get keyword for id. Returns 404, if no such keyword exists."}}]])

(def admin-routes
  ["/keywords"
   {:swagger {:tags ["admin/keywords"] :security [{"auth" []}]}}
   ["/"
    {:get
     {:summary (sd/sum_adm (t "Query keywords"))
      :handler handle_adm-query-keywords
      :middleware [wrap-authorize-admin!]
      :coercion reitit.coercion.schema/coercion
      :parameters {:query schema_query_keyword}
      :responses {200 {:body {:keywords [schema_export_keyword_adm]}}}
      :description "Get keywords id list. TODO query parameters and paging. TODO get full data."}

     :post
     {:summary (sd/sum_adm (t "Create keyword."))
      :coercion reitit.coercion.schema/coercion
      :handler handle_create-keyword
      :middleware [wrap-authorize-admin!]
      :parameters {:body schema_create_keyword}
      ;:responses {200 {:body schema_export_keyword_adm2}
      :responses {200 {:body schema_export_keyword_adm}
                  406 {:body s/Any}}}}]
   ["/:id"
    {:get
     {:summary (sd/sum_adm (t "Get keyword for id"))
      :handler handle_adm-get-keyword
      :middleware [wrap-authorize-admin!
                   wrap-find-keyword]
      :coercion reitit.coercion.schema/coercion

      :parameters {:path {:id s/Uuid}}

      :responses {200 {:body schema_export_keyword_adm}
                  404 {:body s/Any}}
      :description "Get keyword for id. Returns 404, if no such keyword exists."}

     :put
     {:summary (sd/sum_adm (t "Update keyword."))
      :handler handle_update-keyword
      :middleware [wrap-authorize-admin!
                   wrap-find-keyword]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}
                   :body schema_update_keyword}
      :responses {200 {:body schema_export_keyword_adm}
                  404 {:body s/Any}
                  406 {:body s/Any}}}

     :delete
     {:summary (sd/sum_adm (t "Delete keyword."))
      :handler handle_delete-keyword
      :middleware [wrap-authorize-admin!
                   wrap-find-keyword]
      :coercion reitit.coercion.schema/coercion
      :parameters {:path {:id s/Uuid}}
      :responses {200 {:body schema_export_keyword_adm}
                  404 {:body s/Any}
                  ;414 {:body {
                  ;            :message "No such entity in :keywords as :id with dda7c0b5-73e8-4f6d-bbaf-776bf0077389"
                  ;            }}
                  406 {:body s/Any}}}}]])

;### Debug ####################################################################
;(debug/debug-ns *ns*)
