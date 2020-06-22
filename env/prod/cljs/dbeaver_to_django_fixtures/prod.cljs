(ns dbeaver-to-django-fixtures.prod
  (:require [dbeaver-to-django-fixtures.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
