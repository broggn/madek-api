(ns madek.api.pagination
  (:require
   [clojure.tools.logging :as logging]
   [clojure.walk :refer [keywordize-keys]]
   ;; all needed imports
   [honey.sql.helpers :as sql]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]))

(def DEFAULT_LIMIT 1000)

(defn page-number [params]
  (let [page (or (-> params :page) 0)]
    (if (> page 0) page 0)))
  ;(let [page (-> params keywordize-keys :page)]
  ;  (if (not= nil page)
  ;    (let [pagen (Integer/parseInt page)]
  ;      (or pagen 0))
  ;    0)
  ;  )
  ;)

(defn page-count [params]
  (let [count (or (-> params :count) DEFAULT_LIMIT)]
    (if (> count 0) count DEFAULT_LIMIT)))

(defn compute-offset [params]
  (* (page-count params) (page-number params)))

(defn add-offset-for-honeysql [query params]
  (let [defaults {:page 0 :count 100}
        params (merge defaults params)
        p (println ">o> add-offset-for-honeysql params:" params)
        off (compute-offset params)
        limit (page-count params)]
    (-> query
        (sql/limit limit)
        (sql/offset off))))

(defn next-page-query-query-params [query-params]
  (let [query-params (keywordize-keys query-params)
        i-page (page-number query-params)]
    (assoc query-params
           :page (+ i-page 1))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
