(ns sapling.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [response redirect content-type]]
            [sandbar.stateful-session :as ss])
  (:import  [twitter4j Twitter TwitterFactory]
            [twitter4j.conf PropertyConfiguration]))


(def twitter-config
  (PropertyConfiguration. (clojure.java.io/input-stream "/Users/jcowie/Dropbox/nuotltester.properties")))

(defn login []
  (let [twitter (. (TwitterFactory. twitter-config) (getInstance))
        callback-url "http://localhost:7777/callback"
        request-token (. twitter (getOAuthRequestToken callback-url))]
    (ss/session-put! :twitter twitter)
    (ss/session-put! :request-token request-token)
    (redirect (. request-token (getAuthenticationURL)))))

(defn callback [params]
  (let [
        twitter (ss/session-get :twitter)
        request-token (ss/session-get :request-token)
        verifier (:oauth_verifier params)]
    (. twitter (getOAuthAccessToken request-token verifier))
    (let [user (. twitter (showUser (. twitter (getId))))]
      (ss/session-put! :user {:handle (. user (getScreenName)) :name (. user (getName)) :id (. user (getId))})
      (redirect "/"))))

(defn auth [response-function]
    (if (nil? (ss/session-get :user))
      (redirect "/login")
      (response-function)))

(defn home []
  (format "Hello %s" ((ss/session-get :user) :name)))

(defroutes oauth-routes
  (GET "/login" [] (login))
  (GET "/callback" {params :params} (callback params))
  )

(defroutes app-routes
  oauth-routes
  (GET "/" [] (auth home))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      (ss/wrap-stateful-session)))
