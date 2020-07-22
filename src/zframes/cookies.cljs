(ns zframes.cookies
  (:require [goog.net.cookies :as gcookies]
            [re-frame.core :as rf]
            [cljs.reader :as reader]))


(defn get-cookie "Returns the cookie after parsing it with cljs.reader/read-string."
  [k] (reader/read-string (or (.get goog.net.cookies (name k)) "nil")))

(defn set-cookie "Stores the cookie value using pr-str."
  [k v] (.set goog.net.cookies (name k) (pr-str v)))

(defn remove! [k] (.remove goog.net.cookies (name k)))

(rf/reg-cofx
 :cookie/get
 (fn [coeffects key]
   (assoc-in coeffects [:cookie key] (get-cookie key))))

(rf/reg-fx
 :cookie/set
 (fn [{k :key v :value :as opts}]
   (set-cookie k v)))

(rf/reg-fx
 :cookie/remove
 (fn [k] (.remove goog.net.cookies (name k) "/")))
