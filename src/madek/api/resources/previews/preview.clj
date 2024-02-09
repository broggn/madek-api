(ns madek.api.resources.previews.preview
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [madek.api.constants]
   [madek.api.data-streaming :as data-streaming]
   [madek.api.utils.rdbms :as rdbms :refer [get-ds]]
   [madek.api.utils.sql :as sql]))

(defn db-get-preview [id]
  (let [query (-> (sql/select :*)
                  (sql/from :previews)
                  (sql/merge-where
                   [:= :previews.id id])
                  (sql/format))]
    (first (jdbc/query (rdbms/get-ds) query))))

(defn get-preview [request]
  (let [id (-> request :parameters :path :preview_id)
        result (db-get-preview id)]
    (logging/info "get-preview" "\nid\n" id "\nresult\n" result)
    {:body result}))

(defn- preview-file-path [preview]
  (let [
        ; TODO why is this needed for compojure
        ;filename (:filename preview)
        ; TODO why is this needed for reitit

        filename (:filename preview)
        filename (:previews/filename preview)               ;FIXME: handle prefix
        [first-char] filename

        p (println ">o> filename ??" filename)
        p (println ">o> first-char ??" first-char)
        p (println ">o> [first-char] ??" [first-char])
        ]
    (clojure.string/join
     (java.io.File/separator)
     [madek.api.constants/THUMBNAILS_STORAGE_DIR first-char filename])))

(defn get-preview-file-data-stream [request]
  ;(println ">o> request >>>1>>>>" request)
  ;(println ">o> request >>>2>>>>" (get-in request [:previews]))
  ;(println ">o> request >>>3>>>>" (get-in request [:previews :filename]))
  ;(println ">o> request >>>4>>>>" (get-in request [:previews/filename]))
  ;(println ">o> request >>>5>>>>" (:previews/filename request ))
  ;(println ">o> request >>>6>>>>" (:filename request ))
  ;
  ;(println ">o> request >>>2>>>>" (get-in request ["previews"]))
  ;(println ">o> request >>>3>>>>" (get-in request ["previews" "filename"]))
  ;(println ">o> request >>>4>>>>" (get-in request ["previews/filename"]))

  (let [
        ;p (println ">o> >>>>>1>>>>" (:preview request))
        p (println ">o> >>>>>2>>>>" (:previews/filename (:preview request))) ;works
        ;p (println ">o> >>>>>3>>>>" (:filename (:preview request)))
        ;p (println ">o> >>>>>4>>>>" (get-in request [:preview :previews :filename]))
        p (println ">o> >>>>>5>>>>" (get-in request [:preview :previews/filename])) ;works
        ])


  (catcher/snatch {}
                  (when-let [preview (:preview request)]
                    (logging/info "get-preview-file-ds" "\npreview\n" preview)

                    ;(let [file-path (preview-file-path preview)]
                    ;   (println ">o> file-path !!!!" file-path)
                    ;
                    ;  )

                    (when-let [file-path (preview-file-path preview)]
                      (logging/info "get-preview-file-ds" "\nfilepath\n" file-path)
                      (data-streaming/respond-with-file file-path
                                                        ;(:content_type preview))))))
                                                        (:previews/content_type preview) "fromPreview")))))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
