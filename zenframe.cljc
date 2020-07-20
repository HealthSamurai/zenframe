(ns app.zenframe
  (:require [zenframe.core :as z]
            [stylo.core :refer [c]]
            [re-frame.core :as rf]
            [app.pages :as pages]))

(def page-key :zenframe/index)

(defn init
  {:cursor [::page]}
  [{db :db}]
  {:db (assoc db :model "Here")})

(defn on-click
  {:cursor [::page]}
  [{db :db}]
  {:db (update db :inc #(inc (or % 0)))})

(defn model
  {:cursor [::page]}
  [db] db)

(rf/reg-event-ctx page-key (fn [fx & _] (z/>> #'init)))

(defn db-state []
  [:pre (pr-str @z/app-db)])

(z/defv work-view [m #'model]
  [:div {:class (c [:p 2])}
   [:pre {:class (c :display-block [:p 2] [:bg :gray-100])} (pr-str m)]
   #_[:button {:class (c :border [:p 2] [:bg :gray-200])
             :on-click #(z/>> #'on-click)} "Click me"]])

(defn index []
  [:div]
  #_[:div {:class (c [:p 30])}
   [work-view]
   [:hr]
   [db-state]])

(pages/reg-page page-key #'index)

