(ns ytman.models.queries
  (:require [clj-time.jdbc]
            [korma.core :refer [select with fields aggregate where
                                sqlfn order group having limit offset sql-only
                                union queries union-all intersect update set-fields
                                insert values delete join subselect raw modifier]
             :rename {update qupdate}]
            [korma.db :refer [transaction rollback]]
            [clojure.string :refer [trim blank? includes? split join]
             :rename {join strjoin}]
            [clojure.stacktrace :refer [print-stack-trace]]
            [taoensso.timbre :as timbre :refer [debug trace warn]]
            [ytman.models.entities :refer [projects timeslots uploads]]
            [clojure.core.memoize :refer [ttl]]
            [java-time :refer [local-date-time plus days before?]])
  (:import [java.sql SQLIntegrityConstraintViolationException]))

(defn trim! [s]
  (if (nil? s)
    ""
    (trim s)))

(defn get-known-projects []
  (map :projekt
       (select projects
               (fields :projekt))))

(defn get-projects []
  (select projects))

(defn get-project [id]
  (first
   (select projects
           (where {:id id})
           (limit 1))))

(defn register-project [folder]
  (insert projects
          (values {:projekt folder})))

(defn save-project [id lastnumber language playlist playlisturl beschreibung]
  (qupdate projects
           (set-fields {:lastnumber lastnumber
                        :language language
                        :playlist playlist
                        :playlisturl playlisturl
                        :beschreibung beschreibung})
           (where {:id id})))

(defn get-state [file]  
  (first
   (select uploads
           (fields :timei :state)
           (where {:file [like (str "%" file)]}))))

(defn get-last-upload-date [time]
  (get
   (first
    (select timeslots
            (fields :timeslotd)
            (order :timeslotd :DESC)
            (where {:timesloti time})
            (limit 1)))
   :timeslotd
   (local-date-time 2020 06 01)))

(defn get-last-project-time [id]
  (first
   (select projects
           (fields :lasttime :lastdate)
           (where {:id id})
           (limit 1))))

(defn upload-project [id lastnumber lastdate lasttime]
  (qupdate projects
           (set-fields {:lastnumber lastnumber
                        :lasttime lasttime
                        :lastdate lastdate})
           (where {:id id})))

(defn move-timeslot [timei newdate]
  (qupdate timeslots
           (set-fields {:timeslotd newdate})
           (where {:timesloti timei})))

(defn register-uploadtask [file timei config releasedate]
  (insert uploads
          (values {:file file
                   :timei timei
                   :config config
                   :state "NEW"
                   :releasedate releasedate})))

(defn get-next-upload []  
  (first   
   (select uploads
           (fields :file :config :releasedate)
           (order :releasedate :ASC)
           (order :timei :ASC)
           (where {:state "NEW"})
           (limit 1))))

(defn done-upload [file]
  (qupdate uploads
           (set-fields {:state "DONE"})
           (where {:file file})))

(defn progress-upload [file]
  (qupdate uploads
           (set-fields {:state "STARTED"})
           (where {:file file})))

(defn err-upload [file]
  (qupdate uploads
           (set-fields {:state "ERR"})
           (where {:file file})))

(defn reset-upload [file]
  (qupdate uploads
           (set-fields {:state "NEW"})
           (where {:file file})))

(defn delete-upload [file]
  (delete uploads
          (where {:file file
                  :state "DONE"})))
