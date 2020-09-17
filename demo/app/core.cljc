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
  {:cursor [::page]}
  [{db :db}]
  {:db (assoc db :model "Here")})

(defn get-color []
  (let [d (new js/Date)
        ms (.getMilliseconds d)
        base (int (* (/ ms 1000) 256))]
    (str "hsl(" base ", 80%, 50%)")))

(defn on-click
  {:cursor [::page]}
  [{db :db} id]
  {:db (-> db
           (update :inc #(inc (or % 0)))
           (assoc-in [:e id] (get-color)))})

(defn model
  {:cursor [::page]}
  [db] db)

(defn db-state []
  [:pre (pr-str @z/app-db)])

(z/defv work-view [m #'model]
  [:div
   (into
    [:div {:class "wrapper"}]
    (mapv (fn [id]
             [:div
              {:class "button"
               :onMouseMove #(z/>> #'on-click id)
               :style {:background-color (get-in m [:e id])
                       ;;:background-color "hsl(120, 100%, 50%)"
                       }}
              (get-in m [:e id])])
          (range 200)))
   [:pre (pr-str m)]])

(defn index []
  [:<>
   [:style style]
   [:body
    [:div
     [work-view]
     [:hr]
     [db-state]]]])

(defn init! []
   (z/>> #'init)
  #?(:cljs (rd/render [index] (.getElementById js/document "app"))))

(init!)
;; (pages/reg-page page-key #'index)
