(ns madek.api.utils.pagination
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [madek.api.utils.helper :refer [parse-specific-keys]]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def DEFAULT_LIMIT 1000)

(defn page-number [params]
  (let [page (or (-> params :page) 0)]
    (if (> page 0) page 0)))

(defn page-count [params]
  (let [count (or (-> params :count) DEFAULT_LIMIT)]
    (if (> count 0) count DEFAULT_LIMIT)))

(defn compute-offset [params]
  (* (page-count params) (page-number params)))

;(defn parse-specific-keys [params keys-to-parse]
;  (into {}
;    (map (fn [[k v]]
;           [k (if (contains? keys-to-parse k)
;                (do
;                  (println ">o>>>> parse k=>" k)
;                  (Integer/parseInt (str v)))
;                v)])
;      params)))

;(defn parse-to-int [value]
;  (try
;    (Integer/parseInt value)
;    (catch Exception e
;      0)))
;
;(defn parse-specific-keys [params keys-to-parse]
;  (into {}
;    (map (fn [[k v]]
;           [k (if (some #{k} keys-to-parse)
;                (do
;                  (println ">o>>>> parse k=>" k)
;                  (parse-to-int v))
;                (do
;                  (println ">o>>>> nothing to parse k=>" k)
;                  v))])
;      params)))

;
;(defn parse-to-int [value default-value]
;  (try
;    (Integer/parseInt value)
;    (catch Exception e
;      (do
;        (println ">o>>>> failed to parse-to-int: value=>" value ", set default-value=>" default-value )
;        default-value))))
;
;(defn parse-specific-keys [params defaults]
;  (into {}
;    (map (fn [[k v]]
;           [k (if (contains? defaults k)
;                (parse-to-int v (defaults k))
;                v)])
;      params)))
;
;(comment
;  (let [
;        ;res (parse-specific-keys {:page "1" :count "100" :foo "bar"} [:page :count])
;
;        defaults {:page 99 :count 99}
;        res (parse-specific-keys {:page "0" :count "1000" :foo "bar"} defaults)
;
;        p (println ">o> res1" (:page res))
;        p (println ">o> res2" (class (:page res)))
;        ]
;    res
;    )
;  )

(defn sql-offset-and-limit [query params] "Caution: zero-based page numbers"
  (let [defaults {:page 0 :count 1000}
        params (merge defaults params)

        p (println ">o>>>> params.before-toInt =>" params)
        ;params (into {} (map (fn [[k v]] [k (Integer/parseInt (str v))]) params))

        ;params (parse-specific-keys params [:page :count])
        params (parse-specific-keys params defaults)
        ;
        ;p (println ">o>>>> params.after-toInt =>" params)
        ;
        ;p (println ">o> params=>" (:count params))
        ;p (println ">o> params=>" (class (:count params)))
        ;
        ;p (println ">o> params=>" (:count defaults))
        ;p (println ">o> params=>" (class (:count defaults)))
        ;
        ;;p (println ">o> query=>" query)

        p (println ">o> final params for paginationo=" params)

        off (compute-offset params)
        p (println ">o> params.page=>" (:page params))
        ;p (println ">o> off=>" off)

        limit (page-count params)
        ;p (println ">o> params.count=>" (:count params))
        p (println ">o>>>>> limit=>" limit)
        p (println ">o> PAGINATION-DETAIL: offset=" off ", limit=" limit)]
    (-> query
        (sql/offset off)
        (sql/limit limit))

    ;{:offset off :limit limit}
    ))
(comment

  (let [page 1
        page 0
        count 2
        params {:page page :count count}

        res (sql-offset-and-limit nil params)]

    res))

(defn next-page-query-query-params [query-params]
  (let [query-params (keywordize-keys query-params)
        i-page (page-number query-params)]
    (assoc query-params
           :page (+ i-page 1))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
