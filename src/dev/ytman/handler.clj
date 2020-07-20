(ns ytman.handler
  (:require [compojure.core :refer [defroutes GET routes]]
            [hiccup.middleware :refer [wrap-base-url]]            
            [taoensso.timbre :as timbre :refer [info]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]    
            [ytman.routes.data :refer [data-routes]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.route :as route]
            [clojure.pprint]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [clojure.string :refer [includes?]]))
;; => nil


;; /*****************************CONSTRUCTOR AND DESTRUCTOR*************************/

(defn init []
  "called when start"

  (info "ytman is starting"))

(defn destroy []
  "shutting down the app can be used to destruct things"
  (info "ytman is shutting down"))



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
  ["/img/" "/css/" "/__source_changed" "/js/"])
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


(defroutes app-routes
  (route/resources "/")
  (route/not-found {:status 404}))

(defn wrap-print [handler]
  "prints request"
  (fn [request]
    (let [response (handler request)]
      (println request)
      response)))

(def app
  "Stack of APP to execute on requests"
  (-> (routes data-routes app-routes)
      (wrap-log "app" true stripe-keys stripe-headers no-log-paths false) ;; Show all
      (wrap-reload)
      (wrap-refresh ["src"])
      (wrap-params)
      (wrap-base-url)
      (wrap-gzip)))
