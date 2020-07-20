(ns ytman.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [clojure.string :refer [replace] :rename {replace srep}]))

(def version "0.0.1")

(defn common
  "Default layout to storysquid.com"
  [page & body]
  (html5 {:lang "en-en"}
         [:head
          [:title "YTman"]
          [:meta {:charset "utf-8"}]
          [:meta {:name "description" :content ""}]
          [:meta {:name "keywords" :content ""}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
          [:link {:href "https://use.fontawesome.com/releases/v5.8.2/css/all.css"
                  :rel "stylesheet"}]
          [:link {:href "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"
                  :rel "stylesheet"}]
          [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.5.0/css/bootstrap.min.css"
                  ; :integrity "sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh"
                  :crossorigin "anonymous"
                  :rel "stylesheet"}]
          [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/mdbootstrap/4.18.0/css/mdb.min.css"
                  :crossorigin "anonymous"
                  :rel "stylesheet"}]
          ;(include-css "/css/colors.lite.min.css")
          (include-css "/css/screen.css")]
         [:body
          [:div.container-fluid
           body]

          [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js"
                    ; :integrity "sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n"
                    :crossorigin "anonymous"}]
          [:script {:src "https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"
                    :integrity "sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo"
                    :crossorigin "anonymous"}]
          [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.5.0/js/bootstrap.min.js"
                    ; :integrity "sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6"
                    :crossorigin "anonymous"}]
          [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/mdbootstrap/4.18.0/js/mdb.min.js"
                    :crossorigin "anonymous"}]]))