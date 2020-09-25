(ns zenframe.core
  (:require #?(:cljs [reagent.core])
            [zenframe.registrar :as zr]
            [zenframe.dispatch :as zd]
            [zenframe.db :as zdb]))


;;; proxy to lib

(def app-db zdb/app-db)

(defn reg-fx! [id handler-fn]
  (zr/register-handler! :fx id handler-fn))

(defn reg-cofx! [id handler-fn]
  (zr/register-handler! :cofx id handler-fn))

(defn unreg-fx! [id]
  (zr/unregister-handler! :fx id))

(defn unreg-cofx! [id]
  (zr/unregister-handler! :cofx id))

(def >> zd/dispacth)

;;; definition

(defn <<
  "subscribe on sub (z/<< #'sub)"
  [sub & args]
  #?(:cljs
     (let [db (if-let [cursor (:cursor (meta sub))]
                (do
                  (println "cur<<" cursor)
                  (reagent.core/cursor zdb/app-db cursor))
                zdb/app-db)]
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


;;; register builtin fx handlers

(zr/register-handler!
 :fx :>>
 (fn fx-dispatch [ev]
   (apply zd/dispacth ev)))
