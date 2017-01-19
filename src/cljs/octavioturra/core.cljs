(ns octavioturra.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]])
    (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Utils

(defn log [d] (js/console.log (clj->js d)) d)

;; -------------------------
;; Request

(defn load-flow [] (http/get "/flow.json"))

;; -------------------------
;; State

(def state (atom {
  :messages []
  :current-step nil
}))

;; -------------------------
;; Components

(defn button [label on-press] (fn [] [:button {:on-click on-press} label]))

(defn avatar [owner] [:img.avatar {:src owner}])

(defn baloon [from message] (fn [] [:div.balloon [avatar from] [:div.content message]]))

(defn deck [messages] (fn []
  (if (map (fn [{from :from text :text}] [baloon from text]) messages)
      [:div "calma um pouco"]
  )))

;; -------------------------
;; Views

(defn home-page []
  (let [
    {
      messages :messages
      current-step :current-step
    } state
  ] (fn [] [:div [:h2 "Olá, "]
      [deck messages]])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; Initialize app

(defn initialize-messages [first-step] (update-in state [:messages] conj first-step))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (go (let [
          {flow :body} (<! (load-flow))
        ]
        ; (initialize-messages (:start (log flow)))
        (accountant/configure-navigation!
          {:nav-handler
          (fn [path]
            (secretary/dispatch! path))
          :path-exists?
          (fn [path]
            (secretary/locate-route path))})
        (accountant/dispatch-current!)
        (mount-root))))
