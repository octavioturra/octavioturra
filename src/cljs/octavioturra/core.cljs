(ns octavioturra.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-http.client :as http]
              [cljs.core.async :refer [put! chan <! >! timeout close!]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;; -------------------------
;; Utils

(defn log [d] (js/console.log (clj->js d)) d)

(defn now [] (js/Date.))

;; -------------------------
;; State

(def state (atom {
  :messages []
  :flow {}
  :step {}
}))

;; -------------------------
;; Logic

;; processar mensagem
(defn proccess-message! [step]
  (let [text (.-text step)
        owner (.-owner step)] 
          (swap! state update-in [:messages] conj {:text text :owner owner :created-at (now)})))

;; processar variavel
(defn proccess-variable! [step]
  (let [variable (.-variable step)
        value (.-value step)] 
          (swap! state update-in [:value] conj {:variable variable :value value})))

(defn handle-choice [variable value] #(proccess-variable! {:variable variable :value value}))

;; widget-multiple-choice
(defn multiple-choice [action]
  (let [answers (.-answers action)
        variable (.-variable action)] 
          [:form.wideget-multiple-choice
            (map #([:button {:onClick (handle-choice variable %1)} %1]) answers)]))

;; renderizar widget
(def widgets {
  :multiple-choice multiple-choice
  :end (fn [_] [:h1 "obrigado"])
})

(defn render-widget [step] 
  (log step) 
  ((get widgets (.-widget step)) step))

(defn proccess-step [{step :step flow :flow :as state}]
  (let [next (.-next step)
        widget (.-widget step)]
          (proccess-message! step)
          (cond 
            next (proccess-step (aget flow next))
            widget (render-widget step)
            :else (render-widget {:widget :end}))))

;; -------------------------
;; Components

(defn button [label on-press] (fn [] [:button {:on-click on-press} label]))

(defn avatar [owner] [:img.avatar {:src owner}])

(defn baloon [from message] (fn [] [:div.balloon [avatar from] [:div.content message]]))

(defn deck [messages] (let [state @state] 
                        (fn []
                          (if (map (fn [{from :from text :text}] [baloon from text]) messages)
                              [:div "calma um pouco"])
                              (proccess-step state))))

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

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn bootstrap! [flow]
      (swap! state conj {:flow flow :step (.-start flow)})
      (accountant/configure-navigation!
        {:nav-handler
        (fn [path]
          (secretary/dispatch! path))
        :path-exists?
        (fn [path]
          (secretary/locate-route path))})
      (accountant/dispatch-current!)
      (mount-root))

(defn init! [] 
  (js/window.addEventListener "app-loaded" (fn [ev] 
    (let [flow (.-flow (.-detail (js->clj ev)))]
      (bootstrap! flow))))
  (js/window.dataLayer.push (clj->js {:event "app-loaded"})))
