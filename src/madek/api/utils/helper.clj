(ns madek.api.utils.helper
  (:require [clojure.tools.logging :as logging]
            [pghstore-clj.core :refer [to-hstore]]

            [taoensso.timbre :refer [debug info warn error spy]]

            [clojure.walk :refer [postwalk]]                ; Add this line

            )
  (:import (java.util UUID)))

(def LOAD-SWAGGER-DESCRIPTION-FROM-FILE true)
(def LOAD-SWAGGER-DESCRIPTION-FROM-FILE false)

; [madek.api.utils.helper :refer [t d]]
(defn t [s] (str s ".. MANUALLY TESTED"))
(defn d [s] (str s " / doc-example"))
(defn v [s] (str s " / working validation"))
(defn fv [s] (str s " / validation FAILS"))
(defn f
  ([s] (str s " / ToFix"))
  ([s text] (str s " / ToFix: " text)))

; [madek.api.utils.helper :refer [str-to-int]]
(defn str-to-int
  "Attempts to convert a string to an integer, returning a default value if conversion fails."
  [value default-value]
  (println ">o> value=" value ", default-value=" default-value)
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException e
      default-value)))

; [madek.api.utils.helper :refer [mslurp]]
(defn mslurp [file-path]
  (if LOAD-SWAGGER-DESCRIPTION-FROM-FILE
    (slurp file-path)
    "DESCRIPTION DEACTIVATED"))

(comment
  (let [p (println ">o> int" (class 1))

        res (str-to-int "123" 0)
        res (str-to-int 456 0)
        res (str-to-int "x456" 0)
        res (str-to-int {} 0)
        res (str-to-int [] 0)]
    res))

; [madek.api.utils.helper :refer [to-uuid]]
(defn to-uuid
  ([value]

   (println ">o> TO-UUID !!! " value)

   (try
     (let [result (if (instance? String value) (UUID/fromString value) value)]
       result)
     (catch Exception e
       (logging/warn ">>> DEV-ERROR in to-uuid[value], value=" value ", exception=" (.getMessage e))
       value)))

  ([value key]
   (def keys-to-cast-to-uuid #{:user_id :id :group_id :person_id :collection_id :media_entry_id :accepted_usage_terms_id :delegation_id
                               :uploader_id :created_by_id
                               :keyword_id
                               })
   (println ">o> to-uuid[key value]: " value key)
   (println ">o> to-uuid[key value] cl: " value (class value))
   (println ">o> to-uuid[key value] cl: " key (class key))
   (println ">o> to-uuid[key value] castingAllowed?: " (and (contains? keys-to-cast-to-uuid (keyword key)) (instance? String value)))



   (println "------------------------------------")

   (let [

   res (try
     ;(if (and (contains? keys-to-cast-to-uuid key) (instance? String value))
     (if (and (contains? keys-to-cast-to-uuid (keyword key)) (instance? String value))
       (do
         (println ">o> to-uuid[key value] castingDONE!!!!")
         (UUID/fromString value)

         )
       value)
     (catch Exception e
       (logging/warn ">>> DEV-ERROR in to-uuid[value key], value=" value ", key=" key " exception=" (.getMessage e))
       value))
         ]res)

   )

  ([value key table]
   (println ">o> to-uuid[key value table]: " value key table)
   (def blacklisted-tables #{"meta_keys" "vocabularies"})

   (println ">o> keys-to-cast-to-uuid / earlyExitByTableName" table)
   (println ">o> blacklistedTables=" blacklisted-tables)

   ;; XXX: To fix db-exceptions of io_interfaces
   (if (or (contains? blacklisted-tables (name table)) (and (= table :io_interfaces) (= key :id)))
     value
     (to-uuid value key))))

(comment
  (let [;p (println "\nquery ok1" (to-uuid "123e4567-e89b-12d3-a456-426614174000" :user_id))
        ;p (println "\nquery ok1" (class (to-uuid "123e4567-e89b-12d3-a456-426614174000" :user_id)))
        ;

        k "123e4567-e89b-12d3-a456-426614174000"            ;ok
        k "123e"                                            ;error - return val
        ;k 123                                               ;ok - return val

        p (println "\nquery result=" (to-uuid k))
        p (println "\nquery class=" (class (to-uuid k)))

        ;p (println "\nquery result=" (to-uuid k :user_id))
        ;p (println "\nquery class=" (class (to-uuid k :user_id)))
        ]))

;[madek.api.utils.helper :refer [to-uuids]]
(defn to-uuids [ids] (map (fn [id] (if (instance? String id) (UUID/fromString id) id)) ids))

; [madek.api.utils.helper :refer [merge-query-parts]]
(defn merge-query-parts "DEPR" [query-parts]
  (let [placeholder-count (reduce + 0 (map #(count (re-seq #"\?" %)) query-parts))
        required-entries (- (count query-parts) placeholder-count)
        merged (vector (apply str (interpose " " (take required-entries query-parts))))
        remaining (drop required-entries query-parts)]
    (concat merged remaining)))

(defn format-uris [uris]
  (println ">o> format-uris =>" format-uris)
  (clojure.string/join "" (map #(str "{" % "}") uris)))

; [madek.api.utils.helper :refer [urls-to-custom-format]]
;; TODO: maybe possible with json/dump?
(defn convert-to-raw-set [urls]
  (let [transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))

(defn convert-to-raw-array [urls]
  (let [transformed-urls urls
        combined-str (str "' [" (clojure.string/join "," transformed-urls) "]'")]
    [:raw combined-str]))

; [madek.api.utils.helper :refer [convert-map]]
(defn convert-map [map]
  (-> map
      (update :external_uris #(if (nil? %)
                                [:raw "'{}'"]
                                (convert-to-raw-set %)))    ;;rename to convert-to-raw-set

      (update :creator_id #(if (contains? map :creator_id) (to-uuid % :creator_id)))

      ;(update :external_uris #(if (contains? map :external_uris) ([:cast % :varchar])))
      ;(update :start_date #(if (contains? map :start_date) (format-date %)))
      ;(update :end_date #(if (contains? map :end_date) (format-date %)))
      ;(update :inspection_start_date #(if (contains? map :inspection_start_date) (format-date %)))
      ;(update :updated_at #(if (contains? map :updated_at) (format-date %)))
      ))
(defn modify-if-exists [m k f]
  (if (contains? m k)
    (update m k f)
    m))

;; Used for columns of jsonb type
; [madek.api.utils.helper :refer [convert-map-if-exist]]
(defn convert-map-if-exist [m]
  (-> m
      ;; collections: cast to specific db-type
      ;(modify-if-exists :layout #(if (contains? m :layout) (assoc m :layout [:cast % :public.collection_layout])))
      ;(modify-if-exists :default_resource_type #(if (contains? m :default_resource_type) (assoc m :default_resource_type [:cast % :public.collection_default_resource_type])))
      ;(modify-if-exists :sorting #(if (contains? m :sorting) (assoc m :sorting [:cast % :public.collection_sorting])))

      (modify-if-exists :layout #(if (contains? m :layout) [:cast % :public.collection_layout]))
      (modify-if-exists :default_resource_type #(if (contains? m :default_resource_type) [:cast % :public.collection_default_resource_type]))
      (modify-if-exists :sorting #(if (contains? m :sorting) [:cast % :public.collection_sorting]))

      ;; uuid
      (modify-if-exists :id #(if (contains? m :id) (to-uuid % :id)))
      (modify-if-exists :media_entry_default_license_id #(if (contains? m :id) (to-uuid %)))
      (modify-if-exists :edit_meta_data_power_users_group_id #(if (contains? m :edit_meta_data_power_users_group_id) (to-uuid %)))
      (modify-if-exists :creator_id #(if (contains? m :creator_id) (to-uuid %)))
      (modify-if-exists :person_id #(if (contains? m :person_id) (to-uuid %)))
      (modify-if-exists :user_id #(if (contains? m :user_id) (to-uuid %)))
      (modify-if-exists :accepted_usage_terms_id #(if (contains? m :accepted_usage_terms_id) (to-uuid %)))

      (modify-if-exists :created_by_id #(if (contains? m :created_by_id) (to-uuid %)))
      (modify-if-exists :uploader_id #(if (contains? m :uploader_id) (to-uuid %)))
      (modify-if-exists :media_entry_id #(if (contains? m :media_entry_id) (to-uuid %)))


      ;; jsonb / character varying
      (modify-if-exists :settings #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :external_uris #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :sitemap #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :available_locales #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))

      ;(modify-if-exists :json #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))

      ;; text[]
      (modify-if-exists :contexts_for_entry_extra #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_list_details #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_entry_validation #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_dynamic_filters #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_collection_edit #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_collection_extra #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_entry_edit #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_context_keys #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :catalog_context_keys #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :copyright_notice_templates #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))))








;; deprecated
(defn convert-filter-values [filters]
  (let [key-set (set [:uploader_id :created_by_id :media_entry_id])
        transform-if-needed (fn [k v acc]
                              (do
                                (println ">o> ------------------")
                                (println ">o> key:" k)
                                (println ">o> val:" v)
                                (println ">o> acc:" acc)
                                (println ">o> acc:" (:key acc))
                                (println ">o> acc:" (contains? key-set (keyword (:key acc))))
                                (println ">o> ------------------")
                                ;(if (contains? key-set k)
                                (if (contains? key-set (keyword (:key acc)))
                                  ;(to-uuid2 v k)
                                  (to-uuid v)
                                  v)))]                     ; Pass the key to to-uuid
    (postwalk (fn [item]
                (if (map? item)
                  (reduce-kv (fn [acc k v] (assoc acc k (transform-if-needed k v acc)))
                    {}
                    item)
                  item))
      filters)))




(comment
  (let [
        ;filters {:meta_data [{:key "test:string", :match "partial"}
        ;                     {:key "filter:il5r6pws2e1xnhdlgp2b", :value "585d4a10-65ee-42a2-b6e4-abdc2b741c5c"}
        ;                     {:key "test:licenses"}
        ;                     {:not_key "filter:quzcjitujl5lrs24xst5"}
        ;                     {:key "test:people", :value "d35512c3-d9c8-4c8e-96f2-bddc732782e8"}
        ;                     {:key "any", :type "MetaDatum::Keywords", :value "63d0c2b4-97b3-4188-b8c3-96fdf8f7d5e2"}]
        ;         :media_files [{:key "content_type", :value "image/jpeg"}
        ;                       {:key "uploader_id", :value "a323de32-4b77-49de-8bb8-5f20662c9c46"}
        ;                       {:key "extension", :value "jpg"}]
        ;         :permissions [{:key "responsible_user", :value "f1dbdfad-7cec-471a-b710-1d224e384771"}
        ;                       {:key "public", :value true}
        ;                       {:key "entrusted_to_user", :value "3b6261be-cfca-4977-9070-a188c37e2dbd"}
        ;                       {:key "entrusted_to_group", :value "f1146649-d0f9-4cec-8ba3-e4f5857bfd87"}]}

        filters {:meta_data [{:key :test/string, :match "par tial"} {:key :filter/il5r6pws2e1xnhdlgp2b, :value "585d4a10-65ee-42a2-b6e4-abdc2b741c5c"}
                             {:key :test/licenses} {:not_key :filter/quzcjitujl5lrs24xst5} {:key :test/people, :value "d35512c3-d9c8-4c8e-96f2-bddc732782e8"}
                             {:key :any, :type "MetaDatum::Keywords", :value "63d0c2b4-97b3-4188-b8c3-96fdf8f7d5e2"}],
                 :media_files [{:key :content_type, :value "image/jpeg"} {:key :uploader_id, :value "a323de32-4b77-49de-8bb8-5f20662c9c46"}
                               {:key :extension, :value "jpg"}],
                 :permissions [{:key :responsible_user, :value "f1dbdfad-7cec-471a-b710-1d224e384771"}
                               {:key :public, :value true}
                               {:key :entrusted_to_user, :value "3b6261be-cfca-4977-9070-a188c37e2dbd"}
                               {:key :entrusted_to_group, :value "f1146649-d0f9-4cec-8ba3-e4f5857bfd87"}]}


        p (println ">o> " filters)

        res (convert-filter-values filters)

        p (println ">o> " res)
        ]
    res)

  )









(comment
  (let [m {:layout "list"
           :default_resource_type "collections"
           :sorting "manual DESC"}
        res (convert-map-if-exist m)]
    res))

(comment
  (let [map {:external_uris "{mein/link/78}"}
        map {:external_uris "{mein/link/78}"}
        map {:external_uris ["test/me/now/78"]}

        res (convert-map map)]
    res))

; [madek.api.utils.helper :refer [cast-to-hstore]]
(defn cast-to-hstore [data]
  (let [keys [:labels :descriptions :contents :hints :documentation_urls
              :site_titles :brand_texts :welcome_titles :welcome_texts
              :featured_set_titles :featured_set_subtitles :catalog_subtitles :catalog_titles
              :about_pages :support_urls :provenance_notices]]
    (reduce (fn [acc key]
              (if (contains? acc key)
                (let [field-value (get acc key)
                      transformed-value (to-hstore field-value)] ; Assume to-hstore is defined elsewhere
                  (assoc acc key transformed-value))
                acc))
      data
      keys)))

(defn array-to-map [arr]
  (zipmap arr (range (count arr))))
(defn array-to-map [arr]
  (zipmap arr (range (count arr))))

(defn map-to-array [m]
  (map first (sort-by val m)))

;; =================================================================
;; TODO: replace-java-hashmap
;; convert java.*.HashMap to ClolureMap
(defn replace-java-hashmap [v]
  (if (instance? java.util.HashMap v)
    (into {} (for [[k v] v]
               [(keyword k) v]))
    v))

; [madek.api.utils.helper :refer [replace-java-hashmaps]]
(defn replace-java-hashmaps [m]
  (reduce-kv (fn [acc k v]
               (assoc acc k (replace-java-hashmap v)))
    {}
    m))

(def email-regex #"^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$")

; [madek.api.utils.helper :refer [convert-groupid-userid]]
(defn convert-groupid-userid [group-id user-id]
  (let [is_uuid (boolean (re-matches
                           #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
                           group-id))
        group-id (if is_uuid (to-uuid group-id) group-id)

        ;is_email (re-matches #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" user-id)
        is_email (re-matches email-regex user-id)
        is_uuid (boolean (re-matches
                           #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
                           user-id))
        user-id (if is_uuid (to-uuid user-id) user-id)

        is_userid_valid (or is_email is_uuid)
        res {:group-id group-id
             :user-id user-id
             :is_userid_valid is_userid_valid}
        p (println ">o> convert-groupid-userid, result:" res)]
    res))

; [madek.api.utils.helper :refer [convert-userid]]
(defn convert-userid [user-id]
  (let [;is_email (re-matches #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" user-id)

        p (println ">o> user-id=" user-id)
        p (println ">o> user-id.cl=" (class user-id))

        is_email (boolean (re-matches email-regex (str user-id)))

        p (println ">o> is_email_VALID?" is_email)

        is_uuid (boolean (re-matches
                           #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
                           user-id))
        p (println ">o> is_uuid_VALID?" is_uuid)

        user-id (if is_uuid (to-uuid user-id) user-id)

        is_userid_valid (or is_email is_uuid)

        res {:user-id user-id
             :is_userid_valid is_userid_valid
             :is_valid_email is_email
             :is_valid_uuid is_uuid}
        p (println ">o> convert-userid, result:" res)]
    res))

(comment
  (let [mail "_somename@example.com"
        ;mail "somename@example.com"
        ;mail "123e4567-e89b-12d3-a456-426614174000"
        res (convert-userid mail)]
    res))

(defn valid-email? [email]
  (boolean (re-matches email-regex email)))

;; Example usage:
(comment
  (let [email1 "_somename@example.com"
        email2 "somename@example.com"
        valid1 (valid-email? email1)
        valid2 (valid-email? email2)]
    (println "Email 1 is valid:" valid1)
    (println "Email 2 is valid:" valid2)))

; [madek.api.utils.helper :refer [convert-groupid]]
(defn convert-groupid [group-id]
  (let [is_uuid (boolean (re-matches
                           #"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
                           group-id))
        group-id (if is_uuid (to-uuid group-id) group-id)

        res {:group-id group-id}
        p (println ">o> convert-groupid, result:" res)]
    res))

(defn parse-to-int [value default-value]
  (try
    (let [value (if (instance? java.lang.Long value) (str value) value)
          value (if (instance? java.lang.Integer value) (str value) value)

          p (println ">o>> before.parseInt" value)
          p (println ">o>> before.parseInt.class" (class value))]

      ;(Integer/parseInt (str value))
      (Integer/parseInt value))
    (catch Exception e
      (do
        (println ">o>>>> failed to parse-to-int: value=>" value ", set default-value=>" default-value)
        default-value))))

; [madek.api.utils.helper :refer [parse-specific-keys]]
(defn parse-specific-keys [params defaults]
  (into {}
    (map (fn [[k v]]
           [k (if (contains? defaults k)
                (parse-to-int v (defaults k))
                v)])
      params)))

(comment
  (let [;res (parse-specific-keys {:page "1" :count "100" :foo "bar"} [:page :count])

        defaults {:page 99 :count 99}
        res (parse-specific-keys {:page "2" :count "1000" :foo "bar"} defaults)

        p (println ">o> res1" (:page res))
        p (println ">o> res2" (class (:page res)))]
    res))