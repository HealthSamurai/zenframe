(ns app.core
  #?@
   (:clj
    [(:require
      [garden.core :as gc]
      [re-frame.core :as rf]
      [zenframe.core :as z])]
    :cljs
    [(:require
      [garden.core :as gc]
      [re-frame.core :as rf]
      [reagent.dom :as rd]
      [zenframe.core :as z])
     (:require-macros [zenframe.core :as z])]))

(def style
  (gc/css
   [:body
    [:.wrapper {:display "flex"
                :flex-wrap "wrap"}
     [:div.button {:flex "1 1 100px"
                   :border "none"
                   :padding "5px 20px"
                   :text-align "center"
                   :text-decoration "none"
                   :font-size "8px"}]]]))

(def page-key :zenframe/index)

(rf/reg-event-db
  ::init
  (fn [db _] (assoc db :model "Here")))

(defn get-color []
  (let [d (new js/Date)
        ms (.getMilliseconds d)
        base (int (* (/ ms 1000) 256))]
    (str "hsl(" base ", 80%, 50%)")))

(rf/reg-event-db
  ::on-click
  (fn [db [_ id]]
    (-> db
        (update :inc #(inc (or % 0)))
        (assoc-in [:e id] (get-color)))))

(rf/reg-sub
 ::model
 (fn [db] db))

(rf/reg-sub
 ::db
 (fn [db] db))

(defn db-state []
  [:pre (pr-str @(rf/subscribe [::db]))])

(defn work-view
  []
  (let [m @(rf/subscribe [::model])]
    [:div
     (into
      [:div {:class "wrapper"}]
      (mapv (fn [id]
              [:div
               {:class "button"
                :onMouseMove #(rf/dispatch [::on-click id])
                :style {:background-color (get-in m [:e id])
                        ;;:background-color "hsl(120, 100%, 50%)"
                        }}
               (get-in m [:e id])])
            (range 200)))
     [:pre (pr-str m)]]))

(defn index []
  [:html
   [:head
    [:style style]]
   [:body
    [:div
     [work-view]
     [:hr]
      [db-state]]]])

(defn init! []
  (rf/dispatch [::init])
  #?(:cljs (rd/render [index] (.getElementById js/document "app"))))

(init!)
;; (pages/reg-page page-key #'index)
