(ns ytman.routes.data
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.coercions :refer [as-int]]
            [ytman.models.queries :refer [get-known-projects
                                          register-project
                                          get-projects
                                          save-project
                                          get-project
                                          get-state
                                          get-last-upload-date
                                          get-last-project-time
                                          upload-project
                                          move-timeslot
                                          register-uploadtask
                                          delete-upload]]
            [selmer.parser :refer [render-file set-resource-path!]]
            [selmer.util :refer [without-escaping]]
            [ytman.views.layout :refer [common]]
            ;[clj-time.core :refer [plus days before? today]]
            ;[clj-time.coerce :refer [to-date-time]]
            [java-time :refer [plus days before? local-date]]
            [hiccup.form :refer [form-to
                                 hidden-field
                                 text-field
                                 text-area
                                 submit-button]]
            [me.raynes.fs :refer [exists? iterate-dir delete]]
            [taoensso.timbre :as timbre :refer [info trace debug]]
            [clojure.string :refer [ends-with? includes? split replace join escape]
             :rename {replace sreplace}]))

;;(def path "Z:\\Streaming\\")
(def path "/var/www/netshare/")
(def separator "/")

;;(set-resource-path! (clojure.java.io/resource "templates"))
(set-resource-path! "/var/www/templates/")

(defn get-dirs []
  (let [[root _ files] (iterate-dir
                        path)
        [_ dirs _] root
        knownp (get-known-projects)]
    (doseq [dir dirs]
      (when-not
       (some #(= % dir) knownp)
        (register-project dir)))))

(defn mainpage []
  (common "/"
          (let [_ (get-dirs)
                projects (get-projects)
                jumpmark (atom 0)]
            (list             
             [:div.row.mt-3 "Timeslots"]             
             [:div.row
              [:div.col-2 7]
              [:div.col-10 (get-last-upload-date 7)]
              [:div.col-2 12]
              [:div.col-10 (get-last-upload-date 12)]
              [:div.col-2 14]
              [:div.col-10 (get-last-upload-date 14)]
              [:div.col-2 17]
              [:div.col-10 (get-last-upload-date 17)]
              [:div.col-2 22]
              [:div.col-10 (get-last-upload-date 22)]]
             [:div.row.mt-3 "Projekte"]
             (for [{:keys [id projekt lastnumber playlist playlisturl beschreibung language]} projects]
               (list
                (form-to
                 [:post (str "/change#proj" id)]
                 (hidden-field "id" id)
                 (list
                  [:div.row.bg-primary.text-white {:name (str "proj" id)
                                                   :id (str "proj" id)}
                   [:div.col-3 projekt]
                   [:div.col-1 lastnumber]
                   [:div.col-1 language]
                   [:div.col-2 playlist]
                   [:div.col-5 playlisturl]
                   [:div.col-12 beschreibung]]
                  [:div.row.bg-primary.text-white
                   [:div.col-3 ""]
                   [:div.col-1 (text-field "lastnumber" lastnumber)]
                   [:div.col-1 (text-field "language" language)]
                   [:div.col-2 (text-field "playlist" playlist)]
                   [:div.col-5 (text-field "playlisturl" playlisturl)]
                   [:div.col-10 (text-area "beschreibung" beschreibung)]
                   [:div.col-2 (submit-button "Save")]]))
                (let [[_ _ files] (first (iterate-dir (str path projekt separator)))]
                  (for [file (sort files)
                        :let [{:keys [timei state]} (get-state file)]
                        :when (or (ends-with? file ".mp4")
								  (ends-with? file ".mov"))]
                    (form-to
                     [:post (str "/upload#file-" @jumpmark)]
                     (list
                      (hidden-field "id" id)
                      (do 
                        (reset! jumpmark file)
                        (hidden-field "filename" file))
                      [:div.row {:name (str "file-" file)
                                 :id (str "file-" file)}
                       [:div.col-7 file]
                       (cond
                         (nil? timei)
                         (list
                          [:div.col-1 (submit-button {:name "action"} "07")]
                          [:div.col-1 (submit-button {:name "action"} "12")]
                          [:div.col-1 (submit-button {:name "action"} "14")]
                          [:div.col-1 (submit-button {:name "action"} "17")]
                          [:div.col-1 (submit-button {:name "action"} "22")])

                         (= state "NEW")
                         [:div.col-5 timei]

                         (= state "STARTED")
                         [:div.col-5 "Uploading"]

                         (= state "ERR")
                         [:div.col-5 "ERR: " [:iframe {:scr (str "/err/" id "/" file)}]]

                         (= state "DONE")
                         [:div.col-5 (submit-button {:name "action"} "Delete")])]))))))))))


;(get-dirs)
;
(defn do-save-project [id lastnumber language playlist playlisturl beschreibung]
  (save-project id lastnumber language playlist playlisturl beschreibung)
  (mainpage))

(defn get-date [id timei]
  (let [next-free-date (plus (get-last-upload-date timei) (days 1))
        {:keys [lasttime lastdate]} (get-last-project-time id)
        today (local-date)]
    (cond
      (before? lastdate next-free-date)
      ;; Letzter Upload vor Tagen)
      (if (before? next-free-date today)
        ;; Falls dieser vor Tagen war, dann heute
        today
        ;; Sonst den nächsten freien
        next-free-date)

      (and (= next-free-date lastdate)
           (< lasttime timei))
      ;; Gleicher Tag aber danach, dann dieser
      next-free-date

      (before? next-free-date lastdate)
      ;; Frei in vergangenheit dann heute
      today

      (and (= next-free-date lastdate)
           (>= lasttime timei))
      ;; Gleicher Tag aber früher als letztes dann nächster Tag
      (plus next-free-date (days 1))

      :else false)))

(defn upload [id filename action]
  (let [{:keys [projekt lastnumber playlist playlisturl beschreibung language]} (get-project id)
        action-time (as-int action)]
    (cond 
      (= action "Delete")
    ;; Bereinigen
      (do 
        (info (str "Deleting: " path projekt "/" filename))
        (when (exists? (str path projekt "/" filename)) (delete (str path projekt "/" filename)))
        (when (exists? (str path projekt "/" filename ".tpl"))  (delete (str path projekt "/" filename ".tpl")))
        (when (exists? (str path projekt "/" filename ".err"))  (delete (str path projekt "/" filename ".err")))
        (delete-upload filename))
    ;; Uploaden
      (or (= action "07")
          (= action "12")
          (= action "14")
          (= action "17")
          (= action "22"))
      (let [next-date (get-date id action-time)
            this-number (inc lastnumber)
            recorded (first (split filename #"_" 2))]
        (move-timeslot action-time (str next-date))
        (spit (str path projekt "/" filename ".tpl")
              (without-escaping
               (render-file "upload.tpl" 
                            {:TITLE (str projekt " #" (apply str (reverse (take 3 (reverse (str "00" this-number))))))
                             :PLL playlisturl
                             :BE (-> (str beschreibung) 
                                     (sreplace #"\r\n|\n|\r|\n\r" " \\\\n") 
                                     (sreplace  "\"" "'"))
                             :PUB (str next-date "T" action-time ":00:00-03:00") ;; Put your timezone here
                             :DT (if (re-matches #"[0-9]{4}-[0-9]{2}-[0-9]{2}" recorded) recorded (local-date))
                             :PL playlist
                             :LN language})))
        (upload-project id this-number (str next-date) action-time)
        (register-uploadtask (str path projekt "/" filename) 
                             action-time
                             (str path projekt "/" filename ".tpl")
                             next-date))))
  (mainpage))

(defn load-error [id file]
  (let [{:keys [projekt]} (get-project id)]
    (slurp (str path projekt separator file ".err"))))

(defroutes data-routes
  "Define routes of / (home)"
  (GET "/" [] (mainpage))
  (GET "/err/:id/:file" [id :<< as-int
                         file] (load-error id file))
  (POST "/upload" [id filename action] (upload id filename action))
  (POST "/change" [id lastnumber language playlist playlisturl beschreibung] (do-save-project id lastnumber language playlist playlisturl beschreibung)))
