(ns madek.api.utils.pagination
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
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










(defn sql-offset-and-limit [query params] "Caution: zero-based page numbers"
  (let [
        defaults {:page 0 :count 100}
        params (merge defaults params)

        params (into {} (map (fn [[k v]] [k (Integer/parseInt (str v))]) params))


        p (println ">o> params=>" params)
        p (println ">o> params=>" (:count params))
        p (println ">o> params=>" (class (:count params)))

        p (println ">o> params=>" (:count defaults))
        p (println ">o> params=>" (class (:count defaults)))

        ;p (println ">o> query=>" query)
        off (compute-offset params)
        p (println ">o> params.page=>" (:page params))
        ;p (println ">o> off=>" off)

        limit (page-count params)
        p (println ">o> params.count=>" (:count params))
        ;p (println ">o> limit=>" limit)
        p (println ">o> PAGINATION-DETAIL: offset=" off ", limit=" limit)
        ]
    (-> query
        (sql/offset off)
        (sql/limit limit))

    ;{:offset off :limit limit}
    ))


(comment

  (let [

        page 1
        page 0
        count 2
        params {:page page :count count}

        res (sql-offset-and-limit nil params)

        ]
    res
    )

  )

(defn next-page-query-query-params [query-params]
  (let [query-params (keywordize-keys query-params)
        i-page (page-number query-params)]
    (assoc query-params
           :page (+ i-page 1))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
