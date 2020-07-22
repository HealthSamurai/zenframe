(ns zframes.debug
  (:require [re-frame.core  :refer [reg-fx dispatch console] :as rf]))

(rf/reg-fx
 :log
 (fn [value]
   #?(:cljs (js/console.log value))))

(rf/reg-fx
 :print
 (fn [value]
   #?(:cljs (cljs.pprint/pprint value))))
