(defproject ytman "0.0.9" ; -SNAPSHOT
  :description "Upload agent für YT"
  :url "http://easyfix.local/"
  :dependencies [;; Clojure
                 [org.clojure/clojure "1.10.1"]
                 ;; Server
                 [aleph "0.4.6"]
                 ;; Logging
                 [com.taoensso/timbre "4.10.0"]
                 ;; Route handling
                 [compojure "1.6.1"]
                 ;; Client Route handling, currently not used
                 ;[secretary "1.2.3"]
                 ;; Templating
                 [hiccup "1.0.5"]
                 [selmer "1.12.25"]
                 ;; JDBC dependencies & SQL(Lite)
                 [org.clojure/java.jdbc "0.7.11"]
                 [mysql/mysql-connector-java "8.0.19"]
                 ;; [mysql/mysql-connector-java "5.1.45"]
                 ;; DB Abstraktion
                 [korma "0.4.3"]
                 ;; Authentification and Login
                 ;; [com.cemerick/friend "0.2.3"]
                 ;; CSS
                 [garden "1.3.9"]
                 ;; let-try ... catch
                 [try-let "1.3.1"]
                 ;; Sessions
                 [lib-noir "0.9.9"]
                 ;; Deamon Interface
                 [org.apache.commons/commons-daemon "1.0.9"]
                 ;;GZIP support
                 [amalloy/ring-gzip-middleware "0.1.4"]
                 ;;File System ops
                 [me.raynes/fs "1.4.6"]
                 ;; DateTime helpers
                 [clojure.java-time "0.3.2"]
                 ;; Task management
                 [overtone/at-at "1.2.0"]                 
                 ;; Shell Access
                 [clj-commons/conch "0.9.2"]]
  :min-lein-version "2.5.3"
  :source-paths ["src"]
  :plugins [[lein-ring "0.12.5"]
            [lein-garden "0.3.0"]
            [quickie "0.4.2"]]
  :ring {:handler ytman.handler/app
         :init ytman.handler/init
         :destroy ytman.handler/destroy}

  :garden {:builds
           [{:id "main-style"
             :source-paths ["style"]
             :stylesheet css.core/screen
             :compiler {:output-to "resources/public/css/screen.css"
                        :vendors ["webkit" "moz" "o"]
                        :auto-prefix #{:box-shadow}
                        :pretty-print? true}}]}

  :jvm-opts ["-Djava.net.preferIPv4Stack=true" "-Duser.timezone=UTC"
             "-XX:-OmitStackTraceInFastThrow"]

  :target-path "target/%s"

  :test-matcher #"ytman.*\.test\..*"
  :profiles
  {:uberjar {:aot :all
             :main ytman.daemon
             :ring {:open-browser? false
                    :stacktraces? false
                    :auto-reload? false}
             :source-paths ["src/prod"]}
   :dev
   {:dependencies [[ring/ring-mock "0.4.0"]
                   [ring/ring-devel "1.8.0"]
                   [ring-refresh "0.1.2"]
                   [pjstadig/humane-test-output "0.10.0"]]
    :plugins      [[lein-garden "0.3.0"]
                   [quickie "0.4.2"]]
    :ring {:open-browser? false
           :stacktraces? true
           :auto-reload? false}
    :source-paths ["src/dev"]}})