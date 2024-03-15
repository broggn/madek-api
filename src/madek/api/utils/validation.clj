(ns madek.api.utils.validation
  (:require [schema.core :as s]))


;; Define a regular expression for email validation
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")


; [madek.api.utils.validation :refer [email-validation]]
(def email-validation
  (s/constrained s/Str #(re-matches email-regex %)
    "Invalid email address")
  )





(s/defn valid-json?
  ([json-str allow-str-only]
   (if (and (not (= true allow-str-only)) (or (instance? clojure.lang.PersistentVector json-str) (instance? clojure.lang.PersistentHashMap json-str)))
     (do
       (println ">o> ALREADY MAP/ARRAY / valid-json? / class" (class json-str))
       true
       )
     (try
       (println ">o> valid-json? / class" (class json-str))
       (json/read-str json-str)
       (catch Exception e
         false))

     ))

  ([json-str]
   (valid-json? json-str true)
   )

  )

(defn valid-json-native? [json]
  (valid-json? json false)
  )


(def json-and-json-str-validation
  (s/constrained s/Any valid-json-native? "Invalid JSON format")
  )

(def json-str-validation
  (s/constrained s/Any valid-json? "Invalid JSON-STRING format")
  )

