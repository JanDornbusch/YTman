(ns ytman.models.connect
  (:require [korma.db :refer [mysql defdb]]
            [ytman.jdbc]))

;; Bekannte Fehlerquellen:
;; Timezone muss korrekt eingestellt sein am Server!

(defdb db (mysql
           {:db "youtube"
            :user "user"
            :password "password"
            :host "127.0.0.1"}))
