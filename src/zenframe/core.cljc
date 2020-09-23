(ns zenframe.core
  (:require #?(:cljs [reagent.core])
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

(reg-cofx!
 :db
 (fn db-coeffects-handler
   [cofx [_ cursor]]
   (let [edb (if (some? cursor)
               #?(:cljs (reagent.core/cursor app-db cursor)
                  :clj app-db)
               app-db)]
     (assoc cofx :db @edb))))

(defn inject-cofx [cofx ctx]
  (as-> cofx $
    (cond-> $ (not (vector? $)) (vector $))
    ((get-handler :cofx (first $)) ctx cofx)))

(defn inject-cofx* [cofx* ctx]
  ((apply comp (mapv #(partial inject-cofx %) cofx*)) ctx))

;; (defn do-fx* [fx* ctx]
;;   (map))

(defn >>
  "dispatch event (z/>> #'ev {})"
  [ev & args]
  (println ">>" (:name (meta ev)))
  (let [coeffects (-> (:cofx* (meta ev))
                      (conj [:db (:cursor (meta ev))]))
        cofx (inject-cofx* coeffects nil)
        fx (apply ev cofx args)]
    (let [{db :db :as res} (apply ev cofx args)]
      (when db (reset! #?(:cljs (reagent.core/cursor app-db [:app.core/page])
                          :clj app-db) db))
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
