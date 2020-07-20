(ns ytman.repl
  (:require [aleph.http :refer [start-server]]
            [taoensso.timbre :as timbre :refer [trace info error]]
            [overtone.at-at :refer [mk-pool interspaced stop-and-reset-pool!]]
            [clojure.stacktrace :refer [print-stack-trace]]
            [ytman.models.queries :refer [get-next-upload
                                          done-upload
                                          err-upload
                                          progress-upload
                                          reset-upload]]
            [me.raynes.conch.low-level :refer [proc stream-to-string]]
            [java-time :refer [local-date-time as]]
            [clojure.string :refer [includes?]])
  (:use ytman.handler
        [ring.middleware file-info file]))

;; Sollte verifiziert werden md5?
(def path-to-uploader "/var/www/youtubeuploader")

(defonce myserver (atom nil))
(defonce router_ (atom nil))

(defonce tasks (atom {}))
(defonce waiter (atom 0))
(defonce my-task-pool (mk-pool))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (error (str
             "Uncaught exception on: "
             (.getName thread) "\r\n"
             (.getMessage ex) "\r\n"
             (with-out-str (print-stack-trace ex)))))))

(defn task-uploader []
  (let [local-hour (as (local-date-time) :clock-hour-of-day)]
    (info (str "task-uploader executed - " @waiter))
    (if (= 0 @waiter)
      (when (or
	  ;; Upload in a time you not use the internet yourself!
             (< local-hour 10)
             (> local-hour 22))
        (info "Within execution time")
        (let [{:keys [file config]} (get-next-upload)]
          (when-not (empty? file)
            (info (str "Starting to upload: " file))
            (progress-upload file)
            (let [process (proc path-to-uploader "-filename" file "-metaJSON" config)
                  out (future (stream-to-string process :out))]
              (while (not (future-done? out))
                (Thread/sleep 5000))
              (if (includes? @out "Upload successful!")
            ;; Done successful
                (do
                  (info (str file ": successfully uploaded"))
                  (done-upload file))
            ;; Get Error
                (let [err (stream-to-string process :err)]
                  (cond 
				    (includes? err "quotaExceeded")
                    (do 
                      (reset-upload file)
                      (reset! waiter 12))
					
				    (includes? err "connection reset by peer")					
                    (do 
                      (reset-upload file)
                      (reset! waiter 1))
					
    				  :else	
                    (do
                      (error (str file ": error while uploading! \n" err))
                      (err-upload file)
                      (spit (str file ".err") err)))))))))
      (swap! waiter #(dec %)))
    nil))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the myserver is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the myserver picks it up without having to restart.
  (-> #'app
    ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
    ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(defn startup-server
  "used for starting the myserver in development mode from REPL"
  [& [port]]
  ;; test if myserver is running
  (info (str "testing if myserver already running - startup called"))
  (if @myserver
    ;; blocks recreation if running
    (trace "Warning myserver is running!")
    ;; select port if not supplied
    (let [port (if port (Integer/parseInt (str port)) 8080)
          ip "0.0.0.0"
          uri (format "http://localhost:%s/" port)]
      (trace "starting up")
      (reset! myserver
              (start-server (get-handler)
                            {:port port
                      ;:compression? true
                             :local-address (java.net.InetSocketAddress. ip port)
                             :socket-address (java.net.InetSocketAddress. ip port)
                             :remote-address (java.net.InetSocketAddress. ip port)}))
      (info  @myserver)
      (trace "starting up done")
      (swap! tasks assoc
             :task-uploader (interspaced (* 1 60 60 1000) task-uploader my-task-pool))
;;       (try
;;         (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
;;         (catch java.awt.HeadlessException _))
      (info (str "You can view the site at http://localhost:" port)))))

(defn stop-server []
  "used to stop the myserver"
  (when @myserver
    ;; when adds a do here
    (trace "Stopping the myserver")
    (.close @myserver)
    (stop-and-reset-pool!)
    (reset! myserver nil)))
