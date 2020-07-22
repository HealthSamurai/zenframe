(ns zframes.http
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.string :as str]
            [re-frame.db :as db]
            [re-frame.core :as rf]))

(defn sub-query-by-spaces
  [k s] (->> (str/split s #"\s+")
             (mapv (fn [v] (str (name k) "=" v)))
             (str/join "&")))

(defn encode-number-sign [s]
  (str/escape s {\# "%23"}))

(defn to-query [params]
  (->> params
       (mapcat (fn [[k v]]
                 (cond
                   (vector? v) (mapv (fn [vv] (str (name k) "=" vv)) v)
                   (set? v) [(str (name k) "=" (str/join "," v))]
                   :else [(str (name k) "=" v) #_(sub-query-by-spaces k v)])))
       (str/join "&")
       encode-number-sign))

(defn base-url [db url]
  (str (get-in db [:config :base-url]) url))

(defn make-form-data [files]
  (let [form-data (js/FormData.)]
    (doall
     (for [[i file] (map-indexed vector files)]
       (.append form-data (str "file" i) file (str "file" i))))
    form-data))

(defn do-unbundle [data-key map-f resp]
  (if (or (:entry resp) (= "Bundle" (:resourceType resp)))
    (let [data (mapv (if map-f (fn [x] (map-f (:resource x))) :resource) (:entry resp))]
      {data-key data 
       :total (:total resp)})
    {data-key resp}))

(defonce debounce-state (atom {}))

(defn *json-fetch [{:keys [uri format headers params path as map-f process unbundle success error] :as opts}]
  (if (and (:debounce opts) path (not (:force opts)))
    (do
      (when-let [t (get @debounce-state path)]
        (js/clearTimeout t))
      (swap! debounce-state assoc path (js/setTimeout #(*json-fetch (assoc opts :force true)) (:debounce opts))))
    (let [db db/app-db
          ;; token (get-in @db [:auth :data :access_token])
          data-key (or as :data)
          base-url ""
          fmt (or (get {"json" "application/json" "yaml" "text/yaml"} format) "application/json")
          headers (cond-> {"accept" fmt "Cache-Control" "no-cache"}
                    (nil? (:files opts)) (assoc "Content-Type" fmt )
                    true (merge (or headers {})))

          fetch-opts (-> (merge {:method "get" :mode "cors"} opts)
                         (dissoc :uri :headers :success :error :params :files)
                         (assoc :headers headers)
                         (assoc :cache "no-store"))
          fetch-opts (cond-> fetch-opts
                       (:body opts) (assoc :body (if (string? (:body opts)) (:body opts) (.stringify js/JSON (clj->js (:body opts)))))
                       (:files opts) (assoc :body (make-form-data (:files opts))))
          url (str base-url uri)
          dispatch-event (fn [event payload]
                           (when (:event event)
                             (rf/dispatch [(:event event) (merge event {:request opts} payload)])))]

      (when path
        (swap! db assoc-in (conj path :loading) true))

      (->
       (js/fetch (str url (when params (str "?" (to-query params)))) (clj->js fetch-opts))
       (.then
        (fn [resp]
          (if (:dont-parse opts)
            (.then (.text resp)
                   (fn [doc]
                     (if (< (.-status resp) 299)
                       (do (swap! db update-in path merge {:loading false data-key doc})
                           (when success (dispatch-event success {:response resp :data doc})))
                       (do (swap! db update-in path merge {:loading false :error doc})
                           (when error (dispatch-event error {:response resp :data doc})))))
                   ;; No text?
                   (fn [doc]
                     (swap! db update-in path merge {:loading false data-key doc})
                     (when success (dispatch-event success {:response resp :data doc}))))
            (.then (.json resp)
                   (fn [doc]
                     (if (< (.-status resp) 299)
                       (do (swap! db update-in path merge (if unbundle
                                                            (merge {:loading false} (do-unbundle data-key map-f (js->clj doc :keywordize-keys true)))
                                                            {:loading false data-key (js->clj doc :keywordize-keys true)}))
                           (when success
                             (dispatch-event success (if unbundle
                                                       (merge {:response resp} (do-unbundle data-key map-f (js->clj doc :keywordize-keys true)))
                                                       {:response resp :data (js->clj doc :keywordize-keys true)}))))
                       (do
                         (swap! db update-in path merge {:loading false :error (js->clj doc :keywordize-keys true)})
                         (when error (dispatch-event error {:response resp :data (js->clj doc :keywordize-keys true)})))))
                   ;; No json
                   (fn [doc]
                     (swap! db update-in path merge {:loading false :error (js->clj doc :keywordize-keys true)})
                     (when error (dispatch-event error {:response resp :data doc})))))))
       (.catch (fn [err]
                 (.error js/console err)
                 (do (swap! db update-in path merge {:loading false :error {:err err}})
                     (when error (dispatch-event error {:error err})))))))))


(defn json-fetch [opts]
  (if (vector? opts)
    (doseq [o opts] (when (some? o) (*json-fetch o)))
    (*json-fetch opts)))

(rf/reg-fx :http/fetch json-fetch)

(rf/reg-event-fx
 :fetch/success
 (fn [{db :db} [_ {resp :data path :path :as ev}]]
   {:db (assoc-in db path
                  (if (= "Bundle" (:resourceType resp))
                    {:data (mapv :resource (:entry resp))
                     :total (:total resp)
                     :loading false}
                    {:data resp
                     :loading false}))}))

(rf/reg-event-fx
 :fetch/error
 (fn [{db :db} [_ {resp :data path :path :as ev}]]
   {:db (assoc-in db path {:error resp})}))

