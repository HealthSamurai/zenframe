(ns zframes.storage
  (:require [re-frame.core :as rf]))

(defn remove-item
  [key]
  (js/window.localStorage.removeItem (str key)))

(defn set-item
  [key val]
  (js/window.localStorage.setItem
    (str key)
    (-> (clj->js val)
        (js/JSON.stringify)
        (js/encodeURIComponent))))

(defn get-item
  [key]
  (try
    (-> (js/window.localStorage.getItem (str key))
        (js/decodeURIComponent)
        (js/JSON.parse)
        (js->clj :keywordize-keys true))
    (catch :default _
      (remove-item key)
      nil)))

(rf/reg-cofx
  :storage/get
  (fn [coeffects k]
    (assoc-in coeffects [:storage k] (get-item k))))

(rf/reg-fx
  :storage/set
  (fn [items]
    (doseq [[k v] items]
      (set-item k v))))

(rf/reg-fx
  :storage/merge
  (fn [items]
    (doseq [[k v] items]
      (set-item k (merge (get-item k) v)))))

(rf/reg-fx
  :storage/remove
  (fn [keys]
    (doseq [k keys]
      (remove-item (str k)))))
