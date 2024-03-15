(ns madek.api.utils.validation

  (:require [schema.core :as s]

            [clojure.data.json :as json]
            ))


;; Define a regular expression for email validation
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")


; [madek.api.utils.validation :refer [email-validation]]
(def email-validation
  (s/constrained s/Str #(re-matches email-regex %)
    "Invalid email address")
  )

(s/defn valid-vector-or-hashmap?
  ([json-str]
   (or (instance? clojure.lang.PersistentVector json-str) (instance? clojure.lang.PersistentHashMap json-str))
   ))

(s/defn valid-hashmap?
  ([json-str]
    (instance? clojure.lang.PersistentHashMap json-str)
   ))

(s/defn valid-vector?
  ([json-str]
   (instance? clojure.lang.PersistentVector json-str)
   ))



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
  (s/constrained s/Any valid-json-native? "Invalid JSON or JSON-STRING format")
  )

(def json-str-validation
  (s/constrained s/Any valid-json? "Invalid JSON-STRING format")
  )

(def vector-or-hashmap-validation
  (s/constrained s/Any valid-vector-or-hashmap? "Invalid JSON format")
  )
(def json-hashmap-validation
  (s/constrained s/Any valid-hashmap? "Invalid JSON format")
  )
(def json-vector-validation
  (s/constrained s/Any valid-vector? "Invalid JSON format")
  )

