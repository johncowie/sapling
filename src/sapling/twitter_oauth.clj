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
        request-token (. twitter (getOAuthRequestToken (str "http://localhost:7777" callback-url)))]
    (add-to-session :twitter twitter)
    (add-to-session :request-token request-token)
    (redirect (. request-token (getAuthenticationURL)))))

(defn callback [params add-to-session get-from-session]
  (let [
        twitter (get-from-session :twitter)
        request-token (get-from-session :request-token)
        verifier (:oauth_verifier params)]
    (. twitter (getOAuthAccessToken request-token verifier))
    (let [user (. twitter (showUser (. twitter (getId))))]
      (add-to-session :user {:handle (. user (getScreenName)) :name (. user (getName)) :id (. user (getId))})
      (redirect "/"))))

(defmacro defoauthroutes [n login-url callback-url add-to-session get-from-session]
  `(defroutes ~n
    (GET ~login-url [] (login ~add-to-session ~callback-url))
    (GET ~callback-url {params# :params} (callback params# ~add-to-session ~get-from-session))))
