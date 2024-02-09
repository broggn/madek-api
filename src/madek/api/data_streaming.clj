(ns madek.api.data-streaming
  (:require
   [clojure.tools.logging :as logging]
   [ring.util.response]))

(defn respond-with-file [file-path content-type from]

  (println ">o> file-path" file-path)
  (println ">o> content-type " from content-type)

  (if (.exists (clojure.java.io/file file-path))
    (-> (ring.util.response/file-response file-path)
        (ring.util.response/header "X-Sendfile" file-path)
        (ring.util.response/header "content-type" content-type))
    {:status 404 :body {:message (str "File could not be found!\nPath: [" file-path "]")}}))

;### Debug ####################################################################
;(debug/debug-ns 'madek.api.utils.rdbms)
