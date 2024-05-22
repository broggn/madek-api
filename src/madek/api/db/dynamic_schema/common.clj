(ns madek.api.db.dynamic_schema.common
  (:require
   [madek.api.db.dynamic_schema.statics :refer [TYPE_MAYBE TYPE_NOTHING TYPE_OPTIONAL]]
   [madek.api.utils.validation :refer [vector-or-hashmap-validation]]
   [schema.core :as s]



   ))



(def schema-cache (atom {}))
(def enum-cache (atom {}))


(defn get-schema [key & [default]]
  (let [
        val (get @schema-cache key default)
        ;val2 (get @schema-cache (name key) default)
        ;p (println ">o>s get-schema.key=" key)
        ;p (println ">o>s get-schema.val=" val)
        ;p (println ">o>s val2=" val2)

        ;_ (if (nil? val) (System/exit 0 ))
        val (if (nil? val)
              (do
                ;(println ">o> CAUTION !!!!! no-schema-found!!!" key)
                s/Any)
              ;(s/Any)
              ;s/Any

              ;val)
              (into {} val)
              )



        p (println ">o> [get-schema] " key "=" val)

        ] val)
  )


(defn set-schema [key value]
  (let [
        ;; TODO: quiet helpful for debugging
        p (println ">o> !!! [set-schema] (" key ") ->" value)

        value (into {} value)

        res (swap! schema-cache assoc key value)
        ] res)
  )



(defn get-enum [key & [default]]

  (let [
        val (get @enum-cache key default)
        p (println ">o> key=" key)
        p (println ">o> val=" val)
        p (println ">o> default=" default)

        ] val)

  ;(println ">oo> get-enum.key=" key)
  ;(pr key (get @enum-cache key default))
  )


(defn set-enum [key value]
  (println ">oo> set-enum.key=" key)
  (swap! enum-cache assoc key value))
