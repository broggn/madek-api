(ns madek.api.resources.media-entries.advanced-filter.meta-data
  (:require
   ;; all needed imports
   [honey.sql.helpers :as sql]

   [madek.api.utils.helper :refer [to-uuid]]

         ;; all needed imports
               [honey.sql :refer [format] :rename {format sql-format}]
               ;[leihs.core.db :as db]
               [next.jdbc :as jdbc]
               [honey.sql.helpers :as sql]

               [madek.api.db.core :refer [get-ds]]

         [madek.api.utils.helper :refer [array-to-map map-to-array convert-map cast-to-hstore to-uuids to-uuid merge-query-parts]]

   [madek.api.db.core :refer [get-ds]]
   [madek.api.resources.meta-keys.meta-key :as meta-key]
   [next.jdbc :as jdbc]))

(def ^:private match-columns {"meta_data_people" {:table "people",
                                                  :resource "person",
                                                  :match_column "searchable"}
                              "meta_data_keywords" {:table "keywords",
                                                    :resource "keyword",
                                                    :match_column "term"}


                              ;;; TODO: remove this
                              ;"meta_data_text" {:table "keywords",
                              ;                  :resource "keyword",
                              ;                  :match_column "term"}

                              })

(defn- get-meta-datum-object-type [meta-datum-spec]
  (or (:type meta-datum-spec)
    (:meta_datum_object_type
     (first
       (jdbc/execute!
         (get-ds)
         (meta-key/build-meta-key-query (:key meta-datum-spec)))))))

(defn- sql-merge-join-related-meta-data
  ([initial-sqlmap counter related-meta-data-table]
   (sql-merge-join-related-meta-data initial-sqlmap
     counter
     related-meta-data-table
     nil))
  ([initial-sqlmap counter related-meta-data-table match-value]
   (let [meta-data-alias (str "md" counter)
         related-meta-data-alias (str "rmd" counter)]
     (cond-> initial-sqlmap
       related-meta-data-table
       (sql/join [(keyword related-meta-data-table)
                  (keyword related-meta-data-alias)]
         [:=
          (keyword (str related-meta-data-alias ".meta_datum_id"))
          (keyword (str meta-data-alias ".id"))])

       (and related-meta-data-table match-value)
       (->
        (sql/join
          (keyword (get-in match-columns [related-meta-data-table :table]))
          [:=
           (keyword
             (str related-meta-data-alias "."
               (get-in match-columns [related-meta-data-table :resource]) "_id"))
           (keyword
             (str (get-in match-columns [related-meta-data-table :table]) ".id"))]))))))

(defn- sql-merge-where-with-value
  [sqlmap counter related-meta-data-table value]
  (let [related-meta-data-alias (str "rmd" counter)
        column (str (get-in match-columns
                      [related-meta-data-table :resource]) "_id")
        full-column (str related-meta-data-alias "." column)
        p (println ">o> sql-merge-where-with-value, focus on:" full-column)
        ]
    (-> sqlmap
        (sql/where
          [:= full-column (to-uuid value column)]))))

(defn- sql-raw-text-search [column search-string]

   (println ">o> abc!!!!!!!!!!!!!!" )
  ;(sql/raw
  ; we need to pass 'english' because it was also used
  ; when creating indexes


  ;(str "to_tsvector('english', " column ")"
  ;                                      " @@ plainto_tsquery('english', '" search-string "')")




  [:raw (str "to_tsvector('english', "
             column

          ") @@ plainto_tsquery('english', '" search-string "')")]


  )
;)

(defn- sql-merge-where-with-match
  [sqlmap related-meta-data-table match]
  (cond-> sqlmap
    related-meta-data-table
    (sql/where [:raw (str "to_tsvector('english', "
                          (get-in match-columns
                            [related-meta-data-table :table])
                       "."
                       (get-in match-columns
                         [related-meta-data-table :match_column])
                       ") @@ plainto_tsquery('english', '" match "')")])))

;(sql-raw-text-search
; (str (get-in match-columns
;              [related-meta-data-table :table])
;      "."
;      (get-in match-columns
;              [related-meta-data-table :match_column]))
; match))))

(defn- primitive-type? [md-object-type]
  (or (= md-object-type "MetaDatum::Text")
    (= md-object-type "MetaDatum::TextDate")))

(defn- sql-meta-data-from-public-vocabularies [sqlmap meta-data-alias counter]
  (let [meta-keys-alias (str "mk" counter)
        vocabularies-alias (str "v" counter)]
    (-> sqlmap
        (sql/join [:meta_keys (keyword meta-keys-alias)]
          [:= (keyword (str meta-data-alias ".meta_key_id"))
           (keyword (str meta-keys-alias ".id"))])
        (sql/join [:vocabularies (keyword vocabularies-alias)]
          [:and
           [:= (keyword (str meta-keys-alias ".vocabulary_id"))
            (keyword (str vocabularies-alias ".id"))]
           [:= (keyword (str vocabularies-alias ".enabled_for_public_view")) true]]))))

(defn- sql-merge-join-meta-data
  [sqlmap counter md-object-type {meta-key :key
                                  not-meta-key :not_key
                                  match :match}]
  (let [meta-data-alias (str "md" counter)
        join-conditions (cond-> [:and [:=
                                       (keyword (str meta-data-alias
                                                     ".media_entry_id"))
                                       :media_entries.id]]

                          (and (primitive-type? md-object-type) match)
                          (conj (sql-raw-text-search (str meta-data-alias ".string")
                                  match))

                          (not= meta-key "any")
                          (conj [(cond meta-key := not-meta-key :!=)
                                 (keyword (str meta-data-alias ".meta_key_id"))
                                 (or meta-key not-meta-key)]))]

    (-> (sql/join sqlmap
          [:meta_data (keyword meta-data-alias)]
          join-conditions)
        (sql-meta-data-from-public-vocabularies meta-data-alias counter))))

(defn sql-search-through-all [sqlmap search-string]
  (cond-> sqlmap
    search-string
    (sql/where
      (cons :or
        (into [[:exists
                (-> (sql/select true)
                    (sql/from :meta_data)
                    (sql/where [:= :meta_data.media_entry_id :media_entries.id]
                      (sql-raw-text-search "meta_data.string"
                        search-string)))]]
          (map #(let [resource_table (get-in match-columns [% :table])]
                  [:exists
                   (-> (sql/select true)
                       (sql/from (keyword resource_table))
                       (sql/join (keyword %)
                         [:=
                          (keyword (str % "." (get-in match-columns [% :resource]) "_id"))
                          (keyword (str resource_table ".id"))])
                       (sql/join :meta_data
                         [:=
                          (keyword (str % ".meta_datum_id"))
                          :meta_data.id])
                       (sql/where [:raw

                                   (sql-raw-text-search

                                     (str resource_table "."
                                       (get-in match-columns [% :match_column]))
                                     ;") @@ plainto_tsquery('english', '"
                                     search-string)])

                       ; (sql/where
                       ;(sql-raw-text-search
                       ; (str resource_table "."
                       ;      (get-in match-columns [% :match_column]))
                       ; search-string))

                       (sql/where [:= :meta_data.media_entry_id :media_entries.id]))])
            (keys match-columns)))))))


(defn pr [text a]
  (println ">o> pr: " text)
  a
  )

(defn- extend-sqlmap-according-to-meta-datum-spec [sqlmap [meta-datum-spec counter]]
  (let [meta-datum-object-type (get-meta-datum-object-type meta-datum-spec)
        related-meta-data-table (case meta-datum-object-type
                                  "MetaDatum::People" "meta_data_people"
                                  "MetaDatum::Keywords" "meta_data_keywords"


                                  ;; TODO: remove this
                                  ;"MetaDatum::Text" "meta_data_text"

                                  nil)
        sqlmap-with-joined-meta-data (sql-merge-join-meta-data sqlmap
                                       counter
                                       meta-datum-object-type
                                       meta-datum-spec)
        p (println ">o> > sqlmap-with-joined-meta-data.after=" sqlmap-with-joined-meta-data)
        ]



    (println ">o> meta-datum-spec_>" meta-datum-spec)
    (println ">o> meta-datum-spec.match_>" (:match meta-datum-spec))
    (println "------")
    (println ">o> (*) key_>" (:key meta-datum-spec))
    (println ">o> (*) value_>" (:value meta-datum-spec))
    (println "---------------------")


    (cond
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (:key meta-datum-spec)
        (not= (:key meta-datum-spec) "any")
        (:value meta-datum-spec))

      (pr "1" (-> sqlmap-with-joined-meta-data
                  (sql-merge-join-related-meta-data counter related-meta-data-table)
                  (sql-merge-where-with-value counter related-meta-data-table
                    (:value meta-datum-spec))))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (:key meta-datum-spec)
        (not= (:key meta-datum-spec) "any")
        (:match meta-datum-spec))

      ;;test:       {:key "test:string", :match "par tial"}
      (pr "2" (-> sqlmap-with-joined-meta-data
                  (sql-merge-join-related-meta-data counter related-meta-data-table (:match meta-datum-spec))
                  (sql-merge-where-with-match related-meta-data-table (:match meta-datum-spec))
                  ))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (= (:key meta-datum-spec) "any")
        (:value meta-datum-spec)
        (:type meta-datum-spec))

      (pr "3" (-> sqlmap-with-joined-meta-data
                  (sql-merge-join-related-meta-data counter
                    related-meta-data-table)
                  (sql-merge-where-with-value counter
                    related-meta-data-table
                    (:value meta-datum-spec))))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (= (:key meta-datum-spec) "any")
        (:match meta-datum-spec)
        (:type meta-datum-spec))

      (pr "4" (-> sqlmap-with-joined-meta-data
                  (sql-merge-join-related-meta-data counter
                    related-meta-data-table
                    (:match meta-datum-spec))
                  (sql-merge-where-with-match related-meta-data-table
                    (:match meta-datum-spec))))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (= (:key meta-datum-spec) "any")
        (:match meta-datum-spec)
        (not (:type meta-datum-spec)))

      (pr "5" (sql-search-through-all sqlmap (:match meta-datum-spec)))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (:key meta-datum-spec)
        (not (:value meta-datum-spec))
        (not (:match meta-datum-spec)))

      (pr "6" sqlmap-with-joined-meta-data)

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      (and (:not_key meta-datum-spec)
        (not (:value meta-datum-spec))
        (not (:match meta-datum-spec)))

      (pr "7" sqlmap-with-joined-meta-data)

      :else (throw
              (ex-info
                (str "Invalid meta data filter: " meta-datum-spec)
                {:status 422})))))

(defn sql-filter-by [sqlmap meta-data-specs]

  (println ">o> sql-filter-by.meta-data-specs=" meta-data-specs)
  (if-not (empty? meta-data-specs)
    (-> (reduce extend-sqlmap-according-to-meta-datum-spec
          sqlmap
          (partition 2
            (interleave meta-data-specs
              (iterate inc 1))))

        ;(sql/select-distinct) ;; TODO: causes error/duplicate selects
        )

    ;(sql/modifiers :distinct))

    sqlmap))




(comment
  (let [
        sqlmap {:select [[:media_entries.id :media_entry_id] [:media_entries.created_at :media_entry_created_at]
                         [:media_entries.updated_at :media_entry_updated_at] [:media_entries.edit_session_updated_at :media_entry_edit_session_updated_at]
                         [:media_entries.meta_data_updated_at :media_entry_meta_data_updated_at] [:media_entries.is_published :media_entry_is_published]
                         [:media_entries.get_metadata_and_previews :media_entry_get_metadata_and_previews] [:media_entries.get_full_size :media_entry_get_full_size]
                         [:media_entries.creator_id :media_entry_creator_id] [:media_entries.responsible_user_id :media_entry_responsible_user_id]],
                :from [:media_entries], :order-by [[:media_entries.created_at :asc] :media_entries.id],
                :join [:media_files [:= :media_files.media_entry_id :media_entries.id]],
                :where [:and [:= :media_files.content_type "image/jpeg"]
                        [:= :media_files.uploader_id #uuid "64738a30-8f87-496d-94f7-efae597bcc6e"]
                        [:= :media_files.extension "jpg"]
                        [:= :media_entries.responsible_user_id #uuid "97044aa3-51c4-4eaa-b665-85c6ebcd16b5"]
                        [:= :media_entries.get_metadata_and_previews true] [:or [:exists {:select [true], :from [[:media_entry_user_permissions :meup]],
                                                                                          :where [:and [:= :meup.media_entry_id :media_entries.id] [:= :meup.get_metadata_and_previews true] [:= :meup.user_id #uuid "fa7079fe-0495-4663-b759-b07f924fd775"]]}] [:exists {:select [true], :from [[:media_entry_group_permissions :megp]], :where [:and [:= :megp.media_entry_id :media_entries.id] [:= :megp.get_metadata_and_previews true] [:= :gu.user_id #uuid "fa7079fe-0495-4663-b759-b07f924fd775"]], :join [:groups [:= :groups.id :megp.group_id] [:groups_users :gu] [:= :gu.group_id :groups.id]]}]] [:exists {:select [true], :from [[:media_entry_group_permissions :megp]], :where [:and [:= :megp.media_entry_id :media_entries.id] [:= :megp.get_metadata_and_previews true] [:= :megp.group_id #uuid "652da694-88aa-4aae-810e-572e3df3d182"]]}]]}
        ;; TODO: fixme, THIS CAUSES AN ERROR CAUSED BY DUPLICATE ANDS
        ;[SELECT * SELECT DISTINCT  FROM media_entries INNER JOIN meta_data AS md1 ON (md1.media_entry_id = media_entries.id) AND ? AND (md1.meta_key_id = ?)
        map [
             ;{:key "test:string", :match "meta_data_people" :valueXX "par tial"}
             {:key "test:string", :match "Sonny" :valueXX "par tial"}
             ;{:key "test:string", :match :meta_data_people :valueXX "par tial"}


             ;{:key "filter:ovz02sqolgf2qbppoauk", :value #uuid "9f2fad6d-bf14-489c-930e-62c9c81cfa41"} ;;ok
             ;{:key "test:licenses"}                         ;;ok
             ;{:not_key "filter:bbirmzws8bg0huw78uyw"}       ;;ok

             ;{:key (keyword "test:people"), :value (to-uuid "90f347a2-4ab9-4254-bff2-8214e6cdc2f0")}
             ;{:key (keyword "test:people"), :value "90f347a2-4ab9-4254-bff2-8214e6cdc2f0"}
             ;{:key "test:people", :value "90f347a2-4ab9-4254-bff2-8214e6cdc2f0"} ;;ok
             ;{:key "test:people", :value (to-uuid "90f347a2-4ab9-4254-bff2-8214e6cdc2f0")}

             ;{:key "any", :type "MetaDatum::Keywords", :value "a36e7388-0a5f-4bea-b3da-4d872f000100"} ;;ok
             ]

        ;sqlmap {:select [[:media_entries.id :media_entry_id] [:media_entries.created_at :media_entry_created_at]
        ;                 [:media_entries.updated_at :media_entry_updated_at] [:media_entries.edit_session_updated_at :media_entry_edit_session_updated_at]
        ;                 [:media_entries.meta_data_updated_at :media_entry_meta_data_updated_at] [:media_entries.is_published :media_entry_is_published]
        ;                 [:media_entries.get_metadata_and_previews :media_entry_get_metadata_and_previews]
        ;                 [:media_entries.get_full_size :media_entry_get_full_size] [:media_entries.creator_id :media_entry_creator_id]
        ;                 [:media_entries.responsible_user_id :media_entry_responsible_user_id]],
        ;        :from [:media_entries], :order-by [[:media_entries.created_at :asc] :media_entries.id],
        ;        :join [:media_files [:= :media_files.media_entry_id :media_entries.id]],
        ;        :where [:and [:= :media_files.content_type "image/jpeg"] [:= :media_files.uploader_id #uuid "cb819f27-d527-46eb-8d87-00b434a4f009"]
        ;                [:= :media_files.extension "jpg"] [:= :media_entries.responsible_user_id #uuid "446fe14c-06cf-4dde-9bce-4fe3de83d326"]
        ;                [:= :media_entries.get_metadata_and_previews true] [:or [:exists {:select [true], :from [[:media_entry_user_permissions :meup]],
        ;                                                                                  :where [:and [:= :meup.media_entry_id :media_entries.id]
        ;                                                                                          [:= :meup.get_metadata_and_previews true] [:= :meup.user_id #uuid "5dd9609b-e196-4a77-9c09-1852d175a0bc"]]}]
        ;                                                                    [:exists {:select [true], :from [[:media_entry_group_permissions :megp]],
        ;                                                                              :where [:and [:= :megp.media_entry_id :media_entries.id] [:= :megp.get_metadata_and_previews true]
        ;                                                                                      [:= :gu.user_id #uuid "5dd9609b-e196-4a77-9c09-1852d175a0bc"]],
        ;                                                                              :join [:groups [:= :groups.id :megp.group_id] [:groups_users :gu] [:= :gu.group_id :groups.id]]}]]
        ;                [:exists {:select [true], :from [[:media_entry_group_permissions :megp]],
        ;                          :where [:and [:= :megp.media_entry_id :media_entries.id] [:= :megp.get_metadata_and_previews true] [:= :megp.group_id #uuid "30ba5c75-e544-40ed-b06a-6880ab757fd2"]]}]]}


        ;map [
        ;     {:key "test:string", :match "par tial"}
        ;     ;{:key filter:yhl1w6j8p4hzwp7nni9w, :value #uuid "ee22b834-6c26-47c5-a0a3-aad51695ca61"}
        ;     ;{:key test:licenses} {:not_key filter:avu2n5url4h7vzg3u45p}
        ;     ;{:key test:people, :value #uuid "a687bb7b-e6e2-4420-832c-289659c5e44d"}
        ;     ;{:key any, :type MetaDatum::Keywords, :value #uuid "204f48b3-8dfc-431d-a9ee-d2b3aacbd7be"}
        ;     ]

        ;res (sql-filter-by sqlmap map)
        res (sql-filter-by (-> (sql/select :*)
                               (sql/from :media_entries)

                               ) map)

        p (println ">o> 4res=" res)
        res (sql-format res)


        p (println ">o> 3res=" res)

        res (jdbc/execute! (get-ds) res)
        ]
    res)

  )


(comment
  (let [
        match-columns {"meta_data_people" {:table "people",
                                           :resource "person",
                                           :match_column "searchable"}
                       "meta_data_keywords" {:table "keywords",
                                             :resource "keyword",
                                             :match_column "term"}}

        related-meta-data-table "meta_data_people"

        ;; TODO: has to match full term, not part of it
        match "Sonny"

        ab (get-in match-columns [related-meta-data-table :table])
        p (println ">o> ab" ab)


        res (-> (sql/select :*)
                (sql/from :people)

                (sql/where [:raw (str "to_tsvector('english', "
                                      (get-in match-columns
                                        [related-meta-data-table :table])
                                   "."
                                   (get-in match-columns
                                     [related-meta-data-table :match_column])
                                   ") @@ plainto_tsquery('english', '" match "')")])

                sql-format

                )


        p (println ">o> res=" res)

        res (jdbc/execute! (get-ds) res)

        ]
    res)

  )



;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'filter-by-permissions)
;(debug/wrap-with-log-debug #'build-query)
