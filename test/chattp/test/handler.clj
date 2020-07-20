(ns ytman.test.handler
  (:require [ring.middleware.anti-forgery :refer [wrap-anti-forgery]])
  (:use clojure.test
        ring.mock.request
        testapp.handler))

;; Demo nothing here currently
(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
