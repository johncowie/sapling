(ns sapling.twitter-oauth
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :refer [redirect]])
  (:import  [twitter4j Twitter TwitterFactory]
            [twitter4j.conf PropertyConfiguration]))

;; Required Configuration
;; - twitter properties file location
;; - login url
;; - callback url
;; - sometime of wiring in choice of session manager? - maybe do this later

(def twitter-config
  (PropertyConfiguration. (clojure.java.io/input-stream "/Users/jcowie/Dropbox/nuotltester.properties")))

(defn login [add-to-session callback-url]
  (let [twitter (. (TwitterFactory. twitter-config) (getInstance))
        callback-url (str "http://localhost:7777" callback-url)
        request-token (. twitter (getOAuthRequestToken callback-url))]
    (add-to-session :twitter twitter)
    (add-to-session :request-token request-token)
    (redirect (. request-token (getAuthenticationURL)))))

(defn callback [params add-to-session]
  (let [
        twitter (ss/session-get :twitter)
        request-token (ss/session-get :request-token)
        verifier (:oauth_verifier params)]
    (. twitter (getOAuthAccessToken request-token verifier))
    (let [user (. twitter (showUser (. twitter (getId))))]
      (add-to-session :user {:handle (. user (getScreenName)) :name (. user (getName)) :id (. user (getId))})
      (redirect "/"))))

(defmacro defoauthroutes [n login-url callback-url add-to-session]
  `(defroutes ~n
    (GET ~login-url [] (login ~add-to-session ~callback-url))
    (GET ~callback-url {params# :params} (callback params# ~add-to-session))))
