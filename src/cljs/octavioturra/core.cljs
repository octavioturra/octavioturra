(ns octavioturra.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
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
  :events {}
}))

;; -------------------------
;; Broker

(defn emit-gtm [event detail]
  (if js/window.dataLayer
    (js/window.dataLayer.push
      (js/Object.assign
        (clj->js {:event event})
        (clj->js detail)))))

(defn dispatch [event detail]
  (swap! state update-in [:events] conj {:event event :detail detail})
  (emit-gtm event detail)
  (log ["dispatching" event detail])
  (js/window.dispatchEvent
    (js/CustomEvent. event (clj->js {:detail detail}))))

(defn listen [event listener]
  (js/window.addEventListener event
    (fn [ev]
      (let [detail (js->clj (.-detail ev))]
      (log ["receiving" event detail])
      (listener detail)))))

(defn proccess-step [step]
  (let [
    next (get step "next")
    text (get step "text")
    owner (get step "owner")
  ]
    (log ["step" step])
    (when text (dispatch "add-text" {"text" text "owner" owner}))
    (when next (dispatch "set-step" {"step" next}))))

(defn set-step [step-name]
    (let [flow (:flow @state)
          step (get flow step-name)]
      (swap! state assoc-in [:step] step)
      (log ["set-step" step-name flow step @state])
      (proccess-step step)))

(defn add-text [message]
    (swap! state update-in [:messages] conj message))

(defn listen-set-step []
  (listen "set-step"
    (fn [detail]
      (set-step
        (get detail "step")))))

(defn listen-add-text []
  (listen "add-text"
    (fn [detail]
      (let [text (get detail "text")
            owner (get detail "owner")]
        (add-text {:text text :owner owner})))))

(defn listen-answered []
  (listen "answered"
    (fn [detail]
      (let [answer (get detail "answer")
            text (get answer "text")
            next (get answer "next")
            owner "me"]
              (log ["answered" text next])
              (dispatch "add-text" {"text" text "owner" owner})
              (dispatch "set-step" {"step" next})))))

(defn listen-all []
  (listen-add-text)
  (listen-set-step)
  (listen-answered))

(defn button-handler [answer]
  (fn []
    (dispatch "answered" {"answer" answer})))

(defn footer [answers]
    [:footer
      (map-indexed
        (fn [i answer]
          [:button
            {:key i :on-click (button-handler answer)} (get answer "text")])
              answers)])

(defn chat [messages]
    [:article
      (map-indexed (fn [i message] [:div {:key i} [:small (:owner message)] [:br] (:text message)]) messages)])

;; -------------------------
;; Main Component

(defn main [] (fn []
                (let [answers (get (:step @state) "answers")
                      messages (:messages @state)]
                        (log ["main" answers messages @state])
                        [:main
                          [:header "Cabeçalho"]
                          (when messages [chat messages])
                          (when answers [footer answers])])))

;; -------------------------
;; Mounting

(defn mount-root []
  (reagent/render [main] (.getElementById js/document "app")))

(defn bootstrap! [flow]
      (swap! state conj {:flow flow})
      (listen-all)
      (dispatch "set-step" {"step" "start"})
      (mount-root))

(defn init! []
  (listen "app-loaded"
    (fn [detail]
      (let [flow (get detail "flow")]
        (bootstrap! flow))))
  (emit-gtm "app-loaded" {}))
