(ns css.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.color :refer [rgb]]
            [garden.units :refer [px s em]]))

;; Hier wird die screen.css datei definiert.


(defstyles screen
  [:html {:position "relative"
          :min-height "100%"}]

  [:page {:size "A4"}]

  [:body {;/* Margin bottom by footer height */
          :width "100%"
          ;:word-break "break-all"
          :white-space "wrap"
          }]

  [:table 
   {:page-break-inside "auto"}]
  
  [:th {:word-break "normal"
        :white-space "wrap"}]  
  
    ;; And include other styles
  
  [:.breakafter
   {:page-break-after "always"    
    :break-after "page"}]
  )

