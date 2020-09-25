(ns zenframe.registrar)


(def fx&cofx-map "structure that stores effects and coeffects handlers" (atom {}))

(defn register-handler!
  [kind id handler-fn]
  (swap! fx&cofx-map assoc-in [kind id] handler-fn))

(defn unregister-handler!
  [kind id]
  (swap! fx&cofx-map update kind dissoc id))

(defn get-handler
  [kind id]
  (get-in @fx&cofx-map [kind id]))

