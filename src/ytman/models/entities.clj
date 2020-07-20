(ns ytman.models.entities
  (:require [korma.core :refer [defentity pk table database]]
            [ytman.models.connect :refer [db]]))

(declare projects)
(declare timeslots)
(declare uploads)

(defentity projects  
  (database db))

(defentity timeslots
  (database db))

(defentity uploads
  (database db))