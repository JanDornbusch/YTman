(ns ytman.jdbc
  "clojure.java.jdbc protocol extensions supporting DateTime coercion.
  To use in your project, just require the namespace:
    => (require 'clj-time.jdbc)
    nil
  Doing so will extend the protocols defined by clojure.java.jdbc, which will
  cause java.sql.Timestamp objects in JDBC result sets to be coerced to
  org.joda.time.DateTime objects, and vice versa where java.sql.Timestamp
  objects would be required by JDBC."
  (:require [java-time :refer [local-date 
                               local-time
                               local-date-time
                               ]]
            [clojure.java.jdbc :as jdbc]))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (local-date-time v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (local-date v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (local-time v)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
;; (extend-protocol jdbc/ISQLValue
;;   java-time/instant
;;   (sql-value [v]
;;     (to-sql-timestamp v)))