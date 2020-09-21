(ns zenframe.core
  (:require [reagent.core]
            [zenframe.fx-cofx :refer [register-handler! unregister-handler! get-handler]]))

(def app-db
  #?(:clj (atom {})
     :cljs (reagent.core/atom {})))

(defn reg-fx! [id handler-fn]
  (register-handler! :fx id handler-fn))

(defn reg-cofx! [id handler-fn]
  (register-handler! :cofx id handler-fn))

(defn unreg-fx! [id]
  (unregister-handler! :cofx id))

(defn unreg-cofx! [id]
  (unregister-handler! :cofx id))

(defn do-fx [id ]
  )

(defn get-cofx [cofx]
  (get-handler :cofx cofx))

(reg-cofx!
 :db
 (fn db-coeffects-handler
   [coeffects cursor]
   (let [edb (if (some? cursor)
               (do
                 (println "cur>>" cursor)
                 #?(:cljs (reagent.core/cursor app-db cursor)
                    :clj app-db))
               app-db)]
     (assoc coeffects :db @edb))))

(defn inject-cofx [cofx ctx]
  (if (vector? cofx)
    (apply (get-cofx (first cofx)) ctx (rest cofx))
    ((get-cofx cofx) ctx)))

(defn inject-cofx* [cofx* ctx]
  ((apply comp (mapv #(partial inject-cofx %) cofx*)) ctx))

;; (defn do-fx* [fx* ctx]
;;   (map))

(defn >>
  "dispatch event (z/>> #'ev {})"
  [ev & args]
  (let [coeffects (-> (:cofx* (meta ev))
                      (conj [:db (:cursor (meta ev))]))
        cofx (inject-cofx* coeffects nil)]
    (let [{db :db :as res} (apply ev cofx args)]
      (println db)
      (when db (reset! (reagent.core/cursor app-db [:app.core/page]) db))
      res)))

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

(defmacro defv
  [name subs body]
  (let [subs (->> subs
                  (partition 2)
                  (map (fn [[b nm]]
                         (let [n (gensym "sub")]
                           [[n (list 'zenframe.core/<< nm)]
                            [b (list 'deref n)]]))))]
    `(defn ~name
       []
       (let [~@(mapcat first subs)]
         (fn []
           (let [~@(mapcat second subs)]
             ~body))))))
