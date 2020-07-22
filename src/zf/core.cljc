(ns zf.core
  (:require
    [re-frame.core :as rf]
    [clojure.string :as str]))

(defmulti validate (fn [cfg value] (:type cfg)))

(defn assoc-in*
  [m [k & ks] v]
  (let [assoc
        (fn [m k v]
          (if (and (int? k)
                   (or (nil? m) (vector? m))
                   (>= k (count m)))
            (assoc (into (or m []) (repeat (- k (count m)) nil)) k v)
            (assoc m k v)))]
    (if ks
      (assoc m k (assoc-in* (get m k) ks v))
      (assoc m k v))))


(defn get-id** [opts & subpath]
  (str/replace
    (->> (concat (:zf/root opts) (:zf/path opts) subpath)
         (mapv str)
         (str/join "."))
    #"\W+" "_"))

(def get-id (memoize get-id**))


(defn with-path
  [opts & args]
  (apply update opts :zf/path into args))


(defn state-path [{:zf/keys [root path]} & subpath]
  (concat root [:state] path subpath))

(defn schema-path [{:zf/keys [root path]}]
  (concat root [:schema] path))

(defn value-path [{:zf/keys [root path]}]
  (concat root [:value] path))


(defn set-value
  [db opts value]
  (let [value (if (and value (string? value) (str/blank? value)) nil value)]
    (assoc-in* db (value-path opts) value)))

(rf/reg-event-fx
  ::set-value
  (fn [{db :db} [_ opts value]]
    (let [ev (:on-change (get-in db (:zf/root opts)))]
      (cond-> {:db (set-value db opts value)}
        ev (assoc :dispatch ev)))))


(defn merge-state
  [db {root :zf/root path :zf/path} value]
  (update-in db (concat root [:state] path) merge value))

(rf/reg-event-db
  ::merge-state
  (fn [db [_ opts value]]
    (merge-state db opts value)))


(defn set-state
  [db opts inner-path value]
  (update-in db (state-path opts) #(assoc-in % inner-path value)))

(rf/reg-event-db
  ::set-state
  (fn [db [_ opts inner-path value]]
    (set-state db opts inner-path value)))


(defn schema
  [db opts]
  (get-in db (schema-path opts)))

(rf/reg-sub
  ::schema
  (fn [db [_ opts]]
    (schema db opts)))


(defn value
  [db opts]
  (get-in db (value-path opts)))

(rf/reg-sub
  ::value
  (fn [db [_ opts]]
    (value db opts)))


(defn state
  [db opts]
  (get-in db (state-path opts)))

(rf/reg-sub
  ::state
  (fn [db [_ opts]]
    (state db opts)))
