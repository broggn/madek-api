(ns madek.api.main
  (:gen-class)
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :as cli]
   [logbug.catcher :as catcher]
   [logbug.thrown]
   [madek.api.constants]
   [madek.api.db.core :as db]
   [madek.api.utils.config :as config :refer [get-config]]
   [madek.api.utils.exit :as exit]
   [madek.api.utils.logging :as logging]
   [madek.api.utils.nrepl :as nrepl]
   [madek.api.utils.rdbms :as rdbms]
   [madek.api.web]
   [madek.api.web :as web]
   [pg-types.all]
   [taoensso.timbre :refer [info]]))

;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    ["-d" "--dev-mode"]]
   exit/cli-options
   nrepl/cli-options
   web/cli-options
   db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Madek API"
        ""
        "usage: madek-api [<opts>] "
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn helpnexit [summary args options]
  (println (main-usage summary {:args args :options options})))

;; run ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [options]
  (catcher/snatch
   {:level :fatal
    :throwable Throwable
    :return-fn (fn [e] (System/exit -1))}
   (info 'madek.api.main "initializing ...")
   (madek.api.utils.config/initialize
    {:filenames ["./config/settings.yml"
                 "../config/settings.yml",
                 "./datalayer/config/settings.yml",
                 "../webapp/datalayer/config/settings.yml",
                 "./config/settings.local.yml"
                 "../config/settings.local.yml"]})
   (info "Effective startup options " options)
   (info "Effective startup config " (get-config))
    ; WIP switching to new db container; remove old rdbms later
   (rdbms/initialize (config/get-db-spec :api))
   (db/init options)
    ;
   (nrepl/init options)
   (madek.api.constants/initialize (get-config))
   (madek.api.web/initialize options)
   (info 'madek.api.main "... initialized")))

;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce args* (atom nil))

(defn main []
  (logging/init)
  (info "main")
  (let [args @args*]
    (let [args @args*
          {:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options :in-order true)
          options (merge (sorted-map) options)]
      (info "options" options)
      (exit/init options)
      (cond
        (:help options) (helpnexit summary args options)
        :else (run options)))))

(defn -main [& args]
  ;(logbug.thrown/reset-ns-filter-regex #".*madek.*")
  (reset! args* args)
  (main))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; hot reload on require
(when @args* (main))

;### Debug ####################################################################
;(debug/debug-ns 'madek.api.utils.rdbms)
