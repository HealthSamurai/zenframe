(ns zframes.hotkeys
  (:require [re-frame.core :as rf]
            [keybind.core :as key]))

(rf/reg-fx
 :keybind/bind
 (fn [bindings]
   (doseq [[key id event] bindings]
     (key/bind! key id #(rf/dispatch event)))))

(rf/reg-fx
 :keybind/unbind
 (fn [bindings]
   (doseq [[key id] bindings]
     (key/unbind! key id))))
