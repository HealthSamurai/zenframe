(ns zframes.dom
  (:require
    [re-frame.core :as rf]))


(rf/reg-fx
  :focus
  (fn [id]
    #?(:cljs
       (when-let [el (js/document.getElementById id)]
         (.focus el)))))

(rf/reg-event-fx
  :focus
  (fn [fx [_ id]]
    {:focus id}))

(rf/reg-fx
  :into-view
  (fn [id]
    #?(:cljs
       (when-let [el (js/document.getElementById id)]
         (.scrollIntoView el false)))))

(rf/reg-event-fx
  :into-view
  (fn [fx [_ id]]
    {:into-view id}))
