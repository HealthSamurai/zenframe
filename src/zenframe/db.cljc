(ns zenframe.db)


(def app-db
  "application state"
  #?(:clj (atom {})
     :cljs (reagent.core/atom {})))
