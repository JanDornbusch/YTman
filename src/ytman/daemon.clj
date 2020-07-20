(ns ytman.daemon
  (:require [ytman.repl :refer [startup-server stop-server]]
            [taoensso.timbre :as timbre :refer [info]]
            [taoensso.timbre.appenders.core :as appenders])
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class
    :implements [org.apache.commons.daemon.Daemon]))


(def state (atom {}))

(timbre/merge-config!
  {:appenders {:async? true
               :spit (appenders/spit-appender {:fname "/var/log/ytman/timbre.log"})}})

(defn init [args]
  (info "init called: " args)
  (swap! state assoc :running true :started false))

(defn start []
  (info "start called")
  (when-not
   (:started (deref state))
    (swap! state assoc :started true)    
    (startup-server 8081)
    (while
     (:running (deref state))
      (Thread/sleep 5000))))

(defn stop []
  (info "stop called")
  (swap! state assoc :running false :started false)  
  (stop-server))

;; Daemon implementation

(defn -init [this ^DaemonContext context]
  (init (.getArguments context)))

(defn -start [this]
  (future (start)))

(defn -stop [this]
  (stop))

(defn -destroy [this])

;; Enable command-line invocation
(defn -main [& args]
  (init args)
  (start))
