(ns octavioturra.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(def flow {
  :start {
    :text "Oi, sou Octo...",
    :next :greetings
  }
  :greetings {
    :text "Tudo bom?"
    :answers [
      {
        :text "Sim"
        :next :ok
      }
      {
        :text "Não"
        :next :nok
      }
    ]
  }
  :ok {
    :text "Que bom"
    :next :end
  }
  :nok {
    :text "Que pena"
    :next :end
  }
})

;; -------------------------
;; Views

(defn button [label on-press] (fn [] [:button {:on-click on-press} label]))

(defn baloon [from message] (fn [] [:div.balloon ]))

(defn home-page []
  [:div [:h2 "Welcome to octavioturra"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
