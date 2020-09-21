(ns app.core
  #?@
   (:clj
    [(:require [garden.core :as gc] [zenframe.core :as z])]
    :cljs
    [(:require [reagent.dom :as rd]
               [garden.core :as gc]
               [zenframe.core :as z]
               [goog.string :as gstring]
               [goog.string.format])
     (:require-macros [zenframe.core :as z])]))

(def number-of-elements 5)

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

(defn init
  {:cursor [::page]
   :cofx* [:random-color]}
  [{db :db}]
  {:db (-> (reduce #(assoc-in %1 [:e %2] "hsl(10, 80%, 50%)") db (range number-of-elements))
           (assoc :model "Here"))})


(defn get-color-cofx [coeffects _]
  (let [ms #?(:cljs (.getMilliseconds (new js/Date))
              :clj 100)
        base (int (* (/ ms 1000) 256))]
    (assoc coeffects :color (str "hsl(" base ", 80%, 50%)"))))

(z/reg-cofx! :random-color get-color-cofx)

(defn on-click
  {:cursor [::page]
   :cofx* [:random-color]}
  [{db :db color :color} id]
  {:db (-> db
           (update :inc #(inc (or % 0)))
           (assoc-in [:e id] color))})

(defn model
  {:cursor [::page]}
  [db] db)

(defn db-state []
  [:pre (pr-str @z/app-db)])

(z/defv work-view [m #'model]
  [:<>
   (into
    [:div {:class "wrapper"}]
    (mapv (fn [id]
            [:div
             {:class "button"
              :onMouseMove #(z/>> #'on-click id)
              :style {:background-color (get-in m [:e id])}}
             (get-in m [:e id])])
          (range number-of-elements)))
   [:pre (pr-str m)]])

(defn index []
  [:<>
   [:style style]
   [:div
    [work-view]
    [:hr]
    [db-state]]])

(defn init! []
   (z/>> #'init)
  #?(:cljs (rd/render [index] (.getElementById js/document "app"))))

(init!)
;; (pages/reg-page page-key #'index)
