(ns util-create-data-test
  (:require
   [api-test-data :as td]
   [api-test-utils :as tu]
   [clj-http.client :as client]

   [clojure.test :refer [deftest is run-tests run-all-tests]]))

(deftest dev-create-user
  (tu/init-db td/dburltest)
  (tu/del-test-auth2)
  (tu/del-test-user2)
  (tu/del-test-person2)
  (tu/init-test-person2)
  (tu/init-test-user2)
  (tu/init-test-auth2))

(deftest dev-create-user
  (tu/init-db td/dburltest)
  (tu/del-test-auth2)
  (tu/del-test-user2)
  (tu/del-test-person2)
  (tu/init-test-person2)
  (tu/init-test-user2)
  (tu/init-test-auth2))

(deftest dev-create-admin-user
  (tu/init-db td/dburltest)
  (tu/del-test-admin)
  (tu/del-test-auth1)
  (tu/del-test-user)
  (tu/del-test-person)
  (tu/init-test-person)
  (tu/init-test-user)
  (tu/init-test-auth)
  (tu/init-test-admin))

;(deftest test-create-user (tu/init-db td/dburltest) (tu/del-test-user2) (tu/del-test-person2) (tu/init-test-person2) (tu/init-test-user2))

;(deftest test-create-admin-user (tu/init-db td/dburltest) (tu/del-test-admin) (tu/del-test-user) (tu/del-test-person) (tu/init-test-person) (tu/init-test-user) (tu/init-test-admin))

(deftest test-true
  (is true? true))

(run-all-tests)