(ns ring-jetty-ws.listener
  (:gen-class
    :name ring-jetty-ws.Listener
    :extends org.eclipse.jetty.websocket.api.WebSocketAdapter
    :exposes-methods {onWebSocketConnect superOnConnect
                      onWebSocketClose   superOnClose}
    :constructors {[clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn clojure.lang.IFn] []}
    :init init
    :state handlers)
  (:import (ring-jetty-ws Listener)))

(defn handle [^Listener instance key & args]
  (let [handler (get (.handlers instance) key)]
    (apply handler (conj args (.getSession instance)))))

(defn -init [on-connect on-close on-error on-text on-binary]
  [[] {:on-connect on-connect
       :on-close on-close
       :on-error on-error
       :on-text on-text
       :on-binary on-binary}])

(defn -onWebSocketConnect [this ws]
  (.superOnConnect this ws)
  (handle this :on-connect))

(defn -onWebSocketClose [this code reason]
  (handle this :on-close code reason)
  (.superOnClose this code reason))

(defn -onWebSocketError [this cause]
  (handle this :on-error cause))

(defn -onWebSocketText [this msg]
  (handle this :on-text msg))

(defn -onWebsocketBinary [this bytes offset len]
  (handle this :on-binary bytes offset len))
