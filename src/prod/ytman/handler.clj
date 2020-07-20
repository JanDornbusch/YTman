(ns ytman.handler
  (:require [compojure.core :refer [defroutes GET routes]]
            [hiccup.middleware :refer [wrap-base-url]]            
            [taoensso.timbre :as timbre :refer [info]]
            [compojure.route :as route]         
            [ytman.routes.data :refer [data-routes]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.pprint]
            [clojure.java.io :refer [make-parents]]
            [try-let :refer [try-let]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [clojure.string :refer [includes? join]]
            [clojure.stacktrace :refer [print-throwable print-stack-trace]]))


;; /*****************************CONSTRUCTOR AND DESTRUCTOR*************************/

(defn init []
  "called when start"
  (info "ytman is starting"))

(defn destroy []
  "shutting down the app can be used to destruct things"
  (info "ytman is shutting down"))

;; /*****************************LOGGING********************************************/

(defn format-logs [logname request striped-keys striped-headers with-body]
  "used to format logs to console output"
    (let [without-striped-keys (reduce dissoc request striped-keys)
          without-striped-keys-and-headers (reduce (fn [headers to-remove] (update-in headers [:headers] dissoc to-remove)) without-striped-keys striped-headers)
          body (request :body)]
    (with-out-str
      (println logname)
      (clojure.pprint/pprint without-striped-keys-and-headers)
      (when with-body (println "-------------------------------") (clojure.pprint/pprint body))
      (println "***************** END OF LOG *************"))))

(def no-log-paths
  "paths which will not show in console-logs"
  ["/img/" "/css/" "/__source_changed" "/js/out/goog/" "/js/out/taoensso/" "/js/out/figwheel/" "/js/out/cljs/" "/js/out/clojure/"])
(def stripe-nothing-keys
  "keys to remove when no keys are removed"
  [:body])
(def stripe-keys
  "keys to be removed in smallest logs version"
  [:body :keep-alive? :character-encoding :remote-addr :server-name :server-port :ssl-client-cert :scheme :content-type :content-length :query-string :flash])
(def stripe-nothing-headers
  "headers to remove when no headers are removed"
  [])
(def stripe-headers
  "headers to be removed in smallest logs version"
  ["pragma" "host" "user-agent" "accept" "cache-control" "dnt" "user-agent" "accept" "accept-encoding" "accept-language" "accept-charset" "cache-control" "connection" "sec-websocket-key" "sec-websocket-version" "sec-websocket-extensions" "origin" "cookie"
])

(defn wrap-log [handler logname with-body keystriper headerstriper no-log-paths url-spy]
  "wrapper to create logs to console"
  (fn [request]
    (if url-spy (println (request :uri))) ;; if url-spy = true log all paths (urls) accessed to console
    (if (not-any? (fn [x] (includes? (request :uri) x)) no-log-paths) ;; only create full log entry if they are not blacklisted
      (let [incoming (format-logs (str "***** " logname " ***** INcoming request map *****") request keystriper headerstriper false)] ;; There is no body so we cannot log it
        ;; (println incoming) ;; Single message logging
        (let [response (handler request)
              outgoing ""] ;(format-logs (str "***** " logname " ***** OUTgoing response map *****") response keystriper headerstriper with-body)]
            ;; (println outgoing) ;; Single message logging
            (println incoming "\n" outgoing) ;; Double message logging
            response))
    (handler request))))

(defn get-time [toformat]
  "time formatter help to use in logs"
  (-> toformat
      (java.text.SimpleDateFormat.)
      (.format (java.util.Date.))))


;; v 1
;; 30.11.2017 - jan
;; Changed date format - removed the : between yyyy and HH (yyyy:HH) => (yyyy HH)
(defn write-log [efile file request response duration ex]
  "creates logs content and writes it to file (append)"
  (let [log (if (nil? ex)
              (join " " [(get-time "[dd/MM/yyyy HH:mm:ss Z]")
                         duration
                         (get-in request [:headers "x-client-ip"])
                         (get-in request [:headers "x-client-port"])
                         (response :status)
                         (request :uri)
                         ;"nil" ;; user-identifier a cookie can be this (we not use currently)
                         ;"nil" ;; user-id when logged in - not supported yet
                         (request :scheme)
                         (request :request-method)
                         (get-in request [:headers "user-agent"])
                         "\r\n"])
              (join " " [(get-time "[dd/MM/yyyy HH:mm:ss Z]")
                         (request :remote-addr)
                         (request :uri)
                         (.getMessage ex)
                         (print-throwable ex)
                         (print-stack-trace ex)
                         "\r\n"]))]
    (try
      (if (nil? ex)
        (spit file log :append true)
        (spit efile log :append true))
    (catch Exception exlog (println "Exception writing logs: " (.getMessage exlog) " Initial error: " log)))))

(defn wrap-log-to-file [handler path]
  "wrapper to create logs to file"
  (let [file (str path "ytman.log")
        efile (str path "ytman-error.log")]
    (fn [request]
      (make-parents file)
      (let [start (System/currentTimeMillis)]
        (try-let [response (handler request)
                  duration (- (System/currentTimeMillis) start)]
                 (write-log efile file request response duration nil)
                 response
                 (catch Exception ex
                   (write-log efile file request nil nil ex)))))))


;; /*****************************ROUTES*********************************************/

(defroutes app-routes  
  (route/resources "/")
  (route/not-found {:status 404}))

(def app
  "Stack of APP to execute on requests"
  (-> (routes data-routes app-routes)
      (wrap-params)
      (wrap-base-url)
      (wrap-log-to-file "/var/log/aleph/")
      (wrap-gzip)))
