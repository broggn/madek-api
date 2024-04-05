(ns madek.api.resources.media-entries.index
  (:refer-clojure :exclude [str keyword])
  (:require
   [cheshire.core :as json]
   [clojure.core.match :refer [match]]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str :refer [blank?]]
   [clojure.tools.logging :as logging]
   ;; all needed imports
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug :refer [I> I>> identity-with-logging]]
   [madek.api.db.core :refer [get-ds]]
   [madek.api.pagination :as pagination]
   [madek.api.resources.media-entries.advanced-filter :as advanced-filter]
   [madek.api.resources.media-entries.advanced-filter.permissions :as permissions :refer [filter-by-query-params]]
   [madek.api.resources.media-entries.permissions :as media-entry-perms]
   [madek.api.resources.media-files :as media-files]
   [madek.api.resources.meta-data.index :as meta-data.index]
   [madek.api.resources.shared :as sd]
   [madek.api.utils.core :refer [str keyword]]

   [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   ;[leihs.core.db :as db]
   [next.jdbc :as jdbc]))

;### collection_id ############################################################

(defn- filter-by-collection-id [sqlmap {:keys [collection_id] :as query-params}]
  (if-not collection_id
    sqlmap
    (-> sqlmap
        (sql/join [:collection_media_entry_arcs :arcs]
          [:= :arcs.media_entry_id :media_entries.id])
        (sql/where [:= :arcs.collection_id (to-uuid collection_id)])
        (sql/select
          [:arcs.created_at :arc_created_at]
          [:arcs.order :arc_order]
          [:arcs.position :arc_position]
          [:arcs.created_at :arc_created_at]
          [:arcs.updated_at :arc_updated_at]
          [:arcs.id :arc_id]))))

;### query ####################################################################

(defn ^:private base-query [me-query]
  ; TODO make full-data selectable
  (let [sel (sql/select [:media_entries.id :media_entry_id]
              [:media_entries.created_at :media_entry_created_at]
              [:media_entries.updated_at :media_entry_updated_at]
              [:media_entries.edit_session_updated_at :media_entry_edit_session_updated_at]
              [:media_entries.meta_data_updated_at :media_entry_meta_data_updated_at]
              [:media_entries.is_published :media_entry_is_published]
              [:media_entries.get_metadata_and_previews :media_entry_get_metadata_and_previews]
              [:media_entries.get_full_size :media_entry_get_full_size]
              [:media_entries.creator_id :media_entry_creator_id]
              [:media_entries.responsible_user_id :media_entry_responsible_user_id])
        is-pub (:is_published me-query)
        where1 (if (nil? is-pub)
                 sel
                 (sql/where sel [:= :media_entries.is_published (= true is-pub)]))
        creator-id (:creator_id me-query)
        where2 (if (blank? creator-id)                      ; or not uuid
                 where1
                 (sql/where where1 [:= :media_entries.creator_id creator-id]))
        ru-id (:responsible_user_id me-query)
        where3 (if (blank? ru-id)                           ; or not uuid
                 where2
                 (sql/where where2 [:= :media_entries.responsible_user_id ru-id]))

        ; TODO updated/created after
        from (sql/from where3 :media_entries)
        ;        orig-query (-> (sql/select [:media_entries.id :media_entry_id]
        ;                                   [:media_entries.created_at :media_entry_created_at])
        ;                       (sql/from :media_entries))

        p (println ">o> ??? from" from)]

    ;    (logging/info "base-query"
    ;                  "\nme-query:\n" me-query
    ;                  "\nfrom:\n" sel
    ;                  "\nwhere1:\n" where1
    ;                  "\nwhere2:\n" where2
    ;                  "\nwhere3:\n" where3
    ;                  "\nresult:\n" from
    ;                  ;"\norig:\n" orig-query
    ;                  )
    from))

(defn- order-by-media-entry-attribute [query [attribute order]]
  (let [order-by-arg (match [(keyword attribute) (keyword order)]
                       [:created_at :desc] [:media-entries.created_at :desc-nulls-last]
                       [:created_at _] [:media-entries.created_at]
                       [:edit_session_updated_at _] [:media_entries.edit_session_updated_at])]
    (sql/order-by query order-by-arg)))

(defn- order-by-arc-attribute [query [attribute order]]
  (let [order-by-arg (match [(keyword attribute) (keyword order)]
                       [:order :desc] [:arcs.order :desc-nulls-last]
                       [:order _] [:arcs.order]
                       [:position :asc] [:arcs.position :asc]
                       [:position :desc] [:arcs.position :desc-nulls-last]
                       [:created_at :desc] [:arcs.created_at :desc-nulls-last]
                       [:created_at _] [:arcs.created_at])]
    (sql/order-by query order-by-arg)))

(defn- order-by-meta-datum-text [query [meta-key-id order]]
  (let [from-name (-> meta-key-id
                      (clojure.string/replace #"\W+" "_")
                      clojure.string/lower-case
                      (#(str "meta_data_" %)))

        keyword1 (keyword (str from-name ".meta_key_id"))
        keyword2 (keyword (str from-name ".media_entry_id"))
        p (println ">o> meta-key-id=" meta-key-id)
        p (println ">o> from-name=" from-name)
        p (println ">o> keyword1=" keyword1)
        p (println ">o> keyword2=" keyword2)


        ;https://github.com/seancorfield/honeysql/blob/develop/doc/differences-from-1-x.md
        res (-> query
                (sql/left-join [:meta_data from-name]
                  [:= keyword1 meta-key-id])                ;;here FIXME: :asc-nulls-last, ..
                (sql/order-by [(-> from-name (str ".string") keyword)
                               (case (keyword order)
                                 :asc :asc-nulls-last
                                 :desc :desc-nulls-last
                                 :asc-nulls-last)
                               ])
                (sql/where [:= keyword2 :media_entries.id]))]
    res))

(defn- order-reducer [query [scope & more]]
  (println ">o> order-reducer" query)
  (println ">o> order-reducer" scope)
  (case scope
    "media_entry" (order-by-media-entry-attribute query more)
    "arc" (order-by-arc-attribute query more)
    "MetaDatum::Text" (order-by-meta-datum-text query more)))

(defn- order-by-title [query order]
  (let [direction (-> (str/split order #"_") (last))
        reducer (reduce order-reducer [query ["MetaDatum::Text" "madek_core:title" direction]])]
    reducer))

(defn- find-collection-default-sorting [collection-id]
  (let [query {:select [:sorting]
               :from [:collections]
               :where [:= :collections.id collection-id]}]
    (:sorting (jdbc/execute-one! (get-ds) (-> query sql-format)))))

(defn- handle-missing-collection-id [collection-id code-to-run]
  (if (or (not collection-id) (nil? collection-id))
    (throw (ex-info "collection_id param must be given" {:status 422}))
    code-to-run))

(defn- order-by-string [query order collection-id]
  (case order
    "asc" (sql/order-by query [:media_entries.created_at (keyword order)])
    "desc" (sql/order-by query [:media_entries.created_at (keyword order)])
    "title_asc" (order-by-title query order)
    "title_desc" (order-by-title query order)
    "last_change" (order-by-media-entry-attribute query [:edit_session_updated_at :asc])
    "manual_asc" (handle-missing-collection-id collection-id (order-by-arc-attribute query [:position :asc]))
    "manual_desc" (handle-missing-collection-id collection-id (order-by-arc-attribute query [:position :desc]))))

(def ^:private available-sortings '("desc" "asc" "title_asc" "title_desc"
                                    "last_change" "manual_asc" "manual_desc"))

(defn- default-order [query]
  (sql/order-by query [:media_entries.created_at :asc]))

(defn- order-by-collection-sorting [query collection-id]

  (println ">o> order-by-collection-sorting")
  (handle-missing-collection-id collection-id
    (if-let [sorting (find-collection-default-sorting collection-id)]
      (let [prepared-sorting (->> (str/split (str/replace sorting "created_at " "") #" ") (str/join "_") str/lower-case)

            p (println ">o>>>> prepared-sorting=" prepared-sorting)
            p (println ">o>>>> sorting=" sorting)
            my-order (order-by-string query prepared-sorting collection-id)
            p (println ">o>>>> my-order=" my-order)]

        ;;here

        my-order)
      (sql/order-by query [:media_entries.created_at :asc]))))

(def ^:private not-allowed-order-param-message
  (str "only the following values are allowed as order parameter: "
    (str/join ", " available-sortings) " and stored_in_collection"))

(defn- set-order [query query-params]
  (-> (let [qorder (-> query-params :order)
            order (sd/try-as-json qorder)
            collection-id (-> query-params :collection_id)

            p (println ">o> qorder=" qorder)
            p (println ">o> order=" order)
            p (println ">o> order.seq?=" (seq? order))
            p (println ">o> collection-id=" collection-id)

            injected-query query
            p (println ">o> injected-query=" injected-query)

            ;; TODO: this is broken???
            my-cond (cond
                      (nil? order) (default-order query)
                      (string? order) (cond
                                        (some #(= order %) available-sortings) (order-by-string query order collection-id)
                                        (= order "stored_in_collection") (order-by-collection-sorting query collection-id)
                                        :else (throw (ex-info not-allowed-order-param-message
                                                       {:status 422})))
                      (seq? order) (reduce order-reducer query order)
                      :else (default-order query))
            p (println ">o> my-cond=" my-cond)]
        (logging/info "set-order" "\norder\n" order)
        my-cond)
      (sql/order-by :media_entries.id)))

; example queries
;{"meta_data": [{"key": "madek_core:title", "match": "Bildshirmfoto"}],
; "permissions": [{"key":"public","value":"false"}]
; "media-entry": [{"key":"is_published", "value": "true"}
;                 {"key":"creator-id", "value": "" }
;                 {"key": "responsible_user_id", "value":""}]
;}
; test {"meta_data":[{"key":"any","match":"nitai"}]}
; test2 {"meta_data":[{"key":"test:string","match":"par tial"},
;                     {"key":"filter:7cq5ila0xxqlrc7wod2g","value":"3768574c-d4d8-4fac-ad73-0e2dbd4cc443"},
;                     {"key":"test:licenses"},
;                     {"not_key":"filter:1vq1h2t25y92yq8ojg11"},
;                     {"key":"test:people","value":"9f70e42c-8d01-4b2b-8f10-2719921797fc"},
;                     {"key":"any","type":"MetaDatum::Keywords","value":"694b858d-e8eb-4e51-8bee-f55fd8a0491b"}],
;        "media_files":[{"key":"content_type","value":"image/jpeg"},
;                     {"key":"uploader_id","value":"7bf54b03-42e5-4dc8-8a36-309ca9b1563f"},
;                     {"key":"extension","value":"jpg"}],
;        "permissions":[{"key":"responsible_user","value":"935e9257-b7a7-4783-bb11-553907ca67f6"},
;                     {"key":"public","value":"true"},
;                     {"key":"entrusted_to_user","value":"73fbb710-eedc-481d-a411-692705decd09"},
;                     {"key":"entrusted_to_group","value":"e8b962f6-df73-4b6f-b2b6-3f71230cd0aa"}]}



(defn modified-query [query] (-> query
                        (assoc :select-distinct (get query :select)) ;; Copy the select fields to select-distinct
                        (dissoc :select))) ;; Remove the old select key


; TODO test query and paging
(defn- build-query [request]
  (let [query-params (-> request :parameters :query)
        p (println ">o> entries >>>" query-params)
        p (println ">o> --------->>>>><<<<<<-----------")
        filter-by (json/decode (:filter_by query-params) true)
        p (println ">o> entries >>>" filter-by)
        props-by (:media_entry filter-by)
        p (println ">o> entries >>>" props-by)
        authenticated-entity (:authenticated-entity request)

        p (println ">o> entries >>>" authenticated-entity)

        ;p (println ">o> identity-with-logging 1 >>>" identity-with-logging)

        ;p (println ">o> identity-with-logging 2 >>>" (I> identity-with-logging (base-query props-by)))
        p (println ">o> --------------------------")

        ;p (println ">o> identity-with-logging 3 >>>" (I> identity-with-logging (base-query props-by) (set-order query-params)))
        ;p (println ">o> identity-with-logging 4 >>>" (I> identity-with-logging (base-query props-by) (set-order query-params) (filter-by-collection-id query-params)))
        ;p (println ">o> identity-with-logging 5 >>>" (I> identity-with-logging (base-query props-by) (set-order query-params) (filter-by-collection-id query-params) (permissions/filter-by-query-params query-params ;;ok
        ;                                                                                                                                                               authenticated-entity)))

        ;p (println ">o> identity-with-logging 6 >>>" (I> identity-with-logging (advanced-filter/filter-by filter-by)))

        query-res (I> identity-with-logging
                    (base-query props-by)                   ;;ok
                    (set-order query-params)                ;;ok

                    (filter-by-collection-id query-params)  ;;ok
                    (permissions/filter-by-query-params query-params ;;ok
                      authenticated-entity)
                    (advanced-filter/filter-by filter-by)   ;;tofix within filter-by

                    (pagination/add-offset-for-honeysql query-params) ;;ok
                    )

        p (println ">o> HERE!! after query-res >>>" query-res)
        p (println ">o> HERE!! after query-res class >>>" (class query-res))
        ;query-res (-> query-res modified-query sql-format)
        query-res (-> query-res  sql-format)

        p (println ">o> HERE!! after query-res >>>" query-res)]

    ;    (logging/info "build-query"
    ;                  "\nquery-params:\n" query-params
    ;                  "\nfilter-by json:\n" filter-by
    ;                  "\nquery-res:\n" query-res)
    query-res))

(defn- query-index-resources [request]
  (let [
        p (println ">o> query-index-resources HERE !!!!!!!!!!!!!!")
        query (build-query request)
        p (println ">o> query-index-resources.query=" query)

        res (jdbc/execute! (get-ds) query)
        p (println ">o> 22 query-index-resources.res=" res)

        ;>o> 22 query-index-resources.res= [{:responsible_user_id #uuid "344f3884-27a2-4422-b345-da77a08dc8b1", :responsible_delegation_id nil, :get_full_size false, :creator_id #uuid "911d3e2d-d8e3-4138-a53f-005421e4e55b", :updated_at #object[java.time.Instant 0x1d645cea 2024-04-05T08:05:17.311086Z], :edit_session_updated_at #object[java.time.Instant 0x656c144b 2024-04-05T08:05:17.175750Z], :is_published true, :id #uuid "bcd70e44-379d-45d0-bdd5-f0de11203adb", :get_metadata_and_previews true, :meta_data_updated_at #object[java.time.Instant 0x1587c528 2024-04-05T08:05:17.311086Z], :created_at #object[java.time.Instant 0x78dcdde6 2024-04-05T08:05:16.548626Z]} {:responsible_user_id #uuid "f66ede0f-f1de-451e-83bb-f96ebc43d9b6", :responsible_delegation_id nil, :get_full_size false, :creator_id #uuid "013983c8-7ddb-482f-8305-92f58b65e059", :updated_at #object[java.time.Instant 0x47e05d1b 2024-04-05T08:05:17.383500Z], :edit_session_updated_at #object[java.time.Instant 0x422bc7e0 2024-04-05T08:05:17.331966Z], :is_published true, :id #uuid "a8d591de-6efc-43f1-b717-82f2ea97b0b5", :get_metadata_and_previews true, :meta_data_updated_at #object[java.time.Instant 0x692ef408 2024-04-05T08:05:17.383500Z], :created_at #object[java.time.Instant 0x77832726 2024-04-05T08:05:17.313737Z]} {:responsible_user_id #uuid "fec70d23-c350-450d-ad99-41be081b94fa", :responsible_delegation_id nil, :get_full_size false, :creator_id #uuid "46cea0de-7435-4804-8915-855c2f55fd56", :updated_at #object[java.time.Instant 0x324eb448 2024-04-05T08:05:17.550378Z], :edit_session_updated_at #object[java.time.Instant 0x2ec66126 2024-04-05T08:05:17.403590Z], :is_published true, :id #uuid "3bcedaef-7091-499f-8117-116ebb39ed0d", :get_metadata_and_previews true, :meta_data_updated_at #object[java.time.Instant 0x30564e42 2024-04-05T08:05:17.550378Z], :created_at #object[java.time.Instant 0x5e02f579 2024-04-05T08:05:17.385675Z]} {:responsible_user_id #uuid "5add28b0-c76a-4299-adb5-db0a4e1e7974", :responsible_delegation_id nil, :get_full_size false, :creator_id #uuid "c1e6d2d2-e30f-4b6d-b95e-52b79c70db03", :updated_at #object[java.time.Instant 0x19a9e605 2024-04-05T08:05:17.641030Z], :edit_session_updated_at #object[java.time.Instant 0x4fb7b47 2024-04-05T08:05:17.573936Z], :is_published true, :id #uuid "d8476bf7-c527-43a0-b732-d68971d8a606", :get_metadata_and_previews true, :meta_data_updated_at #object[java.time.Instant 0x6dd11c4 2024-04-05T08:05:17.641030Z], :created_at #object[java.time.Instant 0x4f3d955b 2024-04-05T08:05:17.553186Z]} {:responsible_user_id #uuid "9b13b3f4-e743-4d65-b960-babcd8589950", :responsible_delegation_id nil, :get_full_size false, :creator_id #uuid "0f511173-b191-4d2e-a671-ff51c1073fa3", :updated_at #object[java.time.Instant 0x19f2db77 2024-04-05T08:05:17.728327Z], :edit_session_updated_at #object[java.time.Instant 0x468caa4d 2024-04-05T08:05:17.661299Z], :is_published true, :id #uuid "2b01fd46-36df-477c-9cd9-6eae48cf0d9b", :get_metadata_and_previews true, :meta_data_updated_at #object[java.time.Instant 0x28b1d069 2024-04-05T08:05:17.728327Z], :created_at #object[java.time.Instant 0xb051f69 2024-04-05T08:05:17.643317Z]}]

        ] res)
  )

;### index ####################################################################

(defn- get-me-list [full-data data]
  (let [me-list (if (true? full-data)
                  (->> data
                    (map #(select-keys % [:media_entry_id
                                          :media_entry_created_at
                                          :media_entry_updated_at
                                          :media_entry_edit_session_updated_at
                                          :media_entry_meta_data_updated_at
                                          :media_entry_creator_id
                                          :media_entry_responsible_user_id
                                          :media_entry_is_published
                                          :media_entry_get_metadata_and_previews
                                          :media_entry_get_full_size]))
                    (map #(rename-keys % {:media_entry_id :id
                                          :media_entry_created_at :created_at
                                          :media_entry_updated_at :updated_at
                                          :media_entry_edit_session_updated_at :edit_session_updated_at
                                          :media_entry_meta_data_updated_at :meta_data_updated_at
                                          :media_entry_creator_id :creator_id
                                          :media_entry_responsible_user_id :responsible_user_id
                                          :media_entry_is_published :is_published
                                          :media_entry_get_metadata_and_previews :get_metadata_and_previews
                                          :media_entry_get_full_size :get_full_size})))
                  ; else get only ids
                  (->> data
                    (map #(select-keys % [:media_entry_id]))
                    (map #(rename-keys % {:media_entry_id :id}))))]
    ;(logging/info "get-me-list: fd: " full-data " list:" me-list)
    me-list))

(defn get-arc-list [data]

  (let [p (println ">o> data in, data=" data)
        res (->> data
              (map #(select-keys % [:arc_id
                                    :media_entry_id
                                    :arc_order
                                    :arc_position
                                    :arc_created_at
                                    :arc_updated_at]))
              (map #(rename-keys % {:arc_id :id
                                    :arc_order :order
                                    :arc_position :position
                                    :arc_created_at :created_at
                                    :arc_updated_at :updated_at})))
        p (println ">o> data out, data=" res)] res))

(defn- get-files4me-list [melist auth-entity]
  (let [auth-list (remove nil? (map #(when (true? (media-entry-perms/downloadable-by-auth-entity? % auth-entity))
                                       (media-files/query-media-file-by-media-entry-id (:id %))) melist))]
    ;(logging/info "get-files4me-list: \n" auth-list)
    auth-list))

(defn get-preview-list [melist auth-entity]
  (let [auth-list (map #(when (true? (media-entry-perms/viewable-by-auth-entity? % auth-entity))
                          (sd/query-eq-find-all :previews :media_file_id
                            (:id (media-files/query-media-file-by-media-entry-id (:id %))))) melist)]
    ;(logging/info "get-preview-list" auth-list)
    auth-list))

(defn get-md4me-list [melist auth-entity]
  (let [user-id (:id auth-entity)
        auth-list (map #(when (true? (media-entry-perms/viewable-by-auth-entity? % auth-entity))
                          (meta-data.index/get-media-entry-meta-data (:id %) user-id)) melist)]
    auth-list))

(defn build-result [collection-id full-data data]
  (let [me-list (get-me-list full-data data)
        result (merge
                 {:media_entries me-list}
                 (when collection-id
                   {:col_arcs (get-arc-list data)}))]
    result))

(defn build-result-related-data
  "Builds all the query result related data into the response:
  files, previews, meta-data for entries and a collection"
  [collection-id auth-entity full-data data]
  (let [me-list (get-me-list true data)
        result-me-list (get-me-list full-data data)
        user-id (:id auth-entity)
        ; TODO compute only on demand
        files (get-files4me-list me-list auth-entity)
        previews (get-preview-list me-list auth-entity)
        me-md (get-md4me-list me-list auth-entity)
        col-md (meta-data.index/get-collection-meta-data collection-id user-id)
        result (merge
                 {:media_entries result-me-list
                  ; TODO add only on demand
                  :meta_data me-md
                  :media_files files
                  :previews previews}

                 (when collection-id
                   {:col_meta_data col-md
                    :col_arcs (get-arc-list data)}))
        p (println ">o> build-result-related-data.res=" build-result-related-data)]
    result))

(defn get-index [{{{collection-id :collection_id full-data :full_data} :query} :parameters :as request}]
  ;(try
  (catcher/with-logging {}
    (let [data (query-index-resources request)

          p (println ">o> ?? get-index.data=" data)
          p (println ">o> ?? get-index.collection-id=" collection-id)
          p (println ">o> ?? get-index.full-data=" full-data)

          result (build-result collection-id full-data data)

          p (println ">o> 11 final get-index.result=" result)
          ]
      (sd/response_ok result)))
  ;(catch Exception e (sd/response_exception e)))
  )

(defn get-index_related_data [{{{collection-id :collection_id full-data :full_data} :query} :parameters :as request}]
  ;(try
  (catcher/with-logging {}
    (let [auth-entity (-> request :authenticated-entity)
          data (query-index-resources request)
          result (build-result-related-data collection-id auth-entity full-data data)]
      (sd/response_ok result)))
  ;(catch Exception e (sd/response_exception e)))
  )
;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'set-order)
