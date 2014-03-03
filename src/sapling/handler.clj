(ns sapling.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [response redirect content-type]]
            [sandbar.stateful-session :as ss]
            [sapling.twitter-oauth :as oauth])
  (:import  [twitter4j Twitter TwitterFactory]
            [twitter4j.conf PropertyConfiguration]))


;; TODO
;; - separate sandbar session stuff from twitter oauth stuff
;; - pass in callback-url
;; - bundle up oauth routes into a macro

(defn auth [response-function]
    (if (nil? (ss/session-get :user))
      (redirect "/login")
      (response-function)))

(defn home []
  (format "Hello %s" ((ss/session-get :user) :name)))

(oauth/defoauthroutes oauth-routes "/login" "/callback" ss/session-put!)

(defroutes app-routes
  oauth-routes
  (GET "/" [] (auth home))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      (ss/wrap-stateful-session)))
