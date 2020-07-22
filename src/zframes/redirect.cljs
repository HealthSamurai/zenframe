(ns zframes.redirect
  (:require [re-frame.core :as rf]
            [zframes.window-location :as window-location]
            [zframes.routing]
            [re-frame.db]
            [clojure.string :as str]))

(defn page-redirect [url]
  (js/setTimeout (fn [] (set! (.-href (.-location js/window)) url)) 0))

(defn redirect [url]
  (js/setTimeout (fn [] (set! (.-hash (.-location js/window)) url)) 0))

(defn open-in-new-tab [url]
  (js/window.open url "_blank"))

(rf/reg-fx
 ::open-new-tab
 (fn [opts]
   (open-in-new-tab (str (:uri opts
                                     (when-let [params (:params opts)]
                                       (window-location/gen-query-string params)))))))
(defn do-redirect [opts]
  (redirect (str (:uri opts)
                 (when-let [params (:params opts)]
                   (window-location/gen-query-string params)))))
(rf/reg-fx
 ::redirect
 (fn [opts]
   (do-redirect opts)))

(rf/reg-event-fx
 ::redirect
 (fn [fx [_ opts]]
   {::redirect opts}))

(rf/reg-fx
 ::page-redirect
 (fn [opts]
   (page-redirect (str (:uri opts)
                       (when-let [params (:params opts)]
                         (->> params
                              (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))))
                              (str/join "&")
                              (str "?")))))))


(defn set-query-string [params]
  (let [loc (.. js/window -location)]
    (.pushState
     js/history
     #js{} (:title params)
     (str (.-pathname loc)
          (when-let [qs (window-location/gen-query-string (dissoc params :title))]
            qs)
          (.-hash loc)))
    (zframes.routing/dispatch-context nil)))

(comment
  (set-query-string {:title "Aidbox"}))

(rf/reg-fx ::set-query-string set-query-string)

(defn merge-params [{db :db} [_ params]]
  (let [pth (get db :fragment-path)
        nil-keys (reduce (fn [acc [k v]]
                           (if (nil? v) (conj acc k) acc)) [] params)
        old-params (or (get-in db [:fragment-params :params]) {})]
    {::redirect {:uri pth
                 :params (apply dissoc (merge old-params params)
                                nil-keys)}}))

(rf/reg-event-fx ::merge-params merge-params)

(defn set-params [{db :db} [_ params]]
  (let [pth (get db :fragment-path)]
    {::redirect {:uri pth :params params}}))

(rf/reg-event-fx ::set-params set-params)

(defonce params-debounce (atom nil))
(defn set-params-fx [{params :params timeout :debounce}]
  (let [pth (get @re-frame.db/app-db :fragment-path)]
    (if timeout
      (do (when-let [timer @params-debounce]
            (js/clearTimeout timer))
          (reset! params-debounce (js/setTimeout (fn [] (do-redirect {:uri pth :params params})) timeout)))
      (do-redirect {:uri pth :params params}))))

(rf/reg-fx :zfr/set-params set-params-fx)


(rf/reg-fx
  ::back
  (fn []
    (.back js/history)))

(rf/reg-event-fx
  ::back
  (fn [_ _]
    {::back nil}))
