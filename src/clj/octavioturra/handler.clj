(ns octavioturra.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [octavioturra.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:script {:type "text/javascript"} "window.dataLayer = []; (function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
                                      new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
                                      j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
                                      'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
                                      })(window,document,'script','dataLayer','GTM-T8KP5XB');"]
   [:link {:href "https://fonts.googleapis.com/css?family=VT323" :rel "stylesheet"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn cards-page []
  (html5
    (head)
    [:body
     mount-target
     (include-js "/js/app_devcards.js")
     [:noscript "<noscript><iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-T8KP5XB\"
                height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe></noscript>"]]))

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/cards" [] (cards-page))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
