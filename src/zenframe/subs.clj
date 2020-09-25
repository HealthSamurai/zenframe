(ns zenframe.subs
  (:require [reagent.core]))

(defn <<
  "subscribe on sub (z/<< #'sub)"
  [sub & args]
  #?(:cljs
     (let [db (if-let [cursor (:cursor (meta sub))]
                (do
                  (println "cur<<" cursor)
                  (reagent.core/cursor app-db cursor))
                app-db)]
       (reagent.core/track (fn [args] (apply sub @db args)) args))))
