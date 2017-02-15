(ns octavioturra.old-core
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
          (log [@state "before"])
          (swap! state update-in [:messages] conj {:text text :owner owner :created-at (now)})
          (log [@state "after"])))

;; processar variavel
(defn proccess-variable! [step]
  (let [variable (.-variable step)
        value (.-value step)] 
          (swap! state update-in [:value] conj {:variable variable :value value})))

(defn handle-choice [variable answer]
  (fn [] 
    (proccess-variable! {:variable variable :value (.-text answer)})))

;; widget-multiple-choice
(defn multiple-choice [action]
  (let [answers (.-answers action)
        variable (.-variable action)] 
          [:form.wideget-multiple-choice 
            (map-indexed (fn [index answer] (let [
              text (.-text answer)
            ] [:button {:key index :type "button" :onClick (handle-choice variable answer)} text])) answers)]))

;; renderizar widget
(def widgets {
  "multiple-choice" multiple-choice
})

(defn render-widget [step] 
  (if step ((get widgets (.-widget step)) step) [:h1 "obrigado"]))

(defn proccess-step [flow step]
  (let [next (.-next step)
        widget (.-widget step)]
          (log [widget "widget" step @state])
          (proccess-message! step)
          (cond 
            next (proccess-step flow (aget flow next))
            widget (render-widget step)
            :else (render-widget nil))))

;; -------------------------
;; Components

(defn button [label on-press] (fn [] [:button {:on-click on-press} label]))

(defn avatar [owner] [:img.avatar {:src owner}])

(defn baloon [from message] (fn [] [:div.balloon [avatar from] [:div.content message]]))

(defn deck [] (fn [] 
                    [:main [:footer (proccess-step (:flow @state) (:step @state))]
                    [:article (map-indexed (fn [i {owner :owner text :text}] [:div {:key i} [:small owner] [:p text]]) (:messages @state))]]))

;; -------------------------
;; Views

(defn home-page [] [:div [deck]])

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
