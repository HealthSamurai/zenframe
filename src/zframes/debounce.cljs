(ns zframes.debounce
  (:require [re-frame.core  :refer [reg-fx dispatch console] :as rf]))

;; usage example:
;;
;; (reg-event-fx                          ;; note the trailing -fx
;;  :handler-with-debounce                ;; usage:  (dispatch [:handler-with-debounce])
;;  (fn [fx [_ value]]                    ;; the first param will be "world"
;;    {:dispatch-debounce {:key :search
;;                         :event [:search value]
;;                         :delay 250}}))

(defn now [] (.getTime (js/Date.)))

(def registered-keys (atom nil))

(defn dispatch-if-not-superseded [{:keys [key delay event time-received]}]
  (when (= time-received (get @registered-keys key))
    ;; no new events on this key!
    (dispatch event)))

(defn dispatch-later [{:keys [delay] :as debounce}]
  (js/setTimeout
   (fn [] (dispatch-if-not-superseded debounce))
   (or delay 300)))

(defn dispatch-debounce [debounce]
  (let [ts (now)]
    (swap! registered-keys assoc (:key debounce) ts)
    (dispatch-later (assoc debounce :time-received ts))))

(reg-fx :dispatch-debounce dispatch-debounce)

(rf/reg-event-fx
 :dispatch-debounce
 (fn [fx [_ deb]]
   {:dispatch-debounce deb}))
