(ns zenframe.dispatch
  (:require [zenframe.db :as zdb]
            #?(:cljs [reagent.core])
            [zenframe.registrar :as zr]))

(defn inject-db
  [cursor cofx]
  (let [edb (if (some? cursor)
              #?(:cljs (reagent.core/cursor zdb/app-db cursor)
                 :clj zdb/app-db)
              zdb/app-db)]
    (assoc cofx :db @edb)))


(defn inject-cofx [cofx ctx]
  (as-> cofx $
    (cond-> $ (not (vector? $)) (vector $))
    (if-let [handler-fn (zr/get-handler :cofx (first $))]
      (handler-fn ctx cofx)
      cofx)))

(defn inject-cofx* [cofx* ctx]
  ((apply comp (mapv #(partial inject-cofx %) cofx*)) ctx))

(defn do-fx! [[id fx :as m]]
  (if-let [handler-fn (zr/get-handler :fx id)]
    (handler-fn fx)
    (println "handler" id "not registered")))

(defn do-fx*! [fx*]
  (doall (mapv do-fx! fx*)))

(defn mutate-db!
  [new-state cursor]
  (let [edb (if (some? cursor)
              #?(:cljs (reagent.core/cursor zdb/app-db cursor)
                 :clj zdb/app-db)
              zdb/app-db)]
    (if-not (identical? @edb new-state)
      (reset! edb new-state))))

(defn dispacth
  "dispatch event (z/>> #'ev {})"
  [ev & args]
  (println ">>" (:name (meta ev)) "cur:" (:cursor (meta ev)))
  (let [cursor (:cursor (meta ev))
        coeffects (:cofx* (meta ev))
        cofx (->> {}
                  (inject-db cursor)
                  (inject-cofx* coeffects))
        {db :db :as fx} (apply ev cofx args)]
    (mutate-db! db cursor)
    (do-fx*! fx)
    fx))
