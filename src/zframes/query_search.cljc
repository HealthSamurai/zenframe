(ns zframes.query-search
  (:require [re-frame.core :as rf]))

(def query-search-ev ::query-search-ev)
(def query-search ::query-search)
(def on-hash-change ::on-hash-change)

(rf/reg-event-fx
 query-search-ev
 (fn [{db :db} [_ q]]
   {:db (assoc db query-search q)
    :zfr/set-params {:params {:q q} :debounce 300}}))

(rf/reg-sub
 query-search
 (fn [db _] (get db query-search)))

(rf/reg-event-fx
 on-hash-change
 (fn [{db :db} [_ q]]
   (when-not (= (query-search db) q)
     {:db (assoc db query-search q)})))
