(ns ring-jetty-ws.ws
  (:require [clojure.string :as string])
  (:import (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler HandlerList)
           (org.eclipse.jetty.websocket.servlet WebSocketServletFactory WebSocketCreator ServletUpgradeResponse)
           (org.eclipse.jetty.websocket.server WebSocketHandler)
           (java.util Locale)
           (javax.servlet.http HttpServletRequest)
           (org.eclipse.jetty.websocket.api Session)
           (ring-jetty-ws Listener)
           (java.nio ByteBuffer)))

(defprotocol WebSocket
  "A websocket connection protocol"
  (send! [this msg]
    "Sends a string message to the client.")
  (send-bin! [this ^ByteBuffer data]
    "Sends binary data to the client.")
  (close! [this] [this status reason]
    "Closes the websocket connection.")
  (connected? [this]
    "Returns true if the websocket is still connected.")
  (unwrap [this]
    "Ideally you won't need this, but the current API is pretty basic,
     so in case you need to do something more complicated, this returns
     the underlying org.eclipse.jetty.websocket.api.Session"))

(extend-protocol WebSocket
  Session
  (send! [this text]
    (.sendString (.getRemote this) text))
  (send-bin! [this data]
    (.sendBytes (.getRemote this) data))
  (close! [this]
    (.close this))
  (close! [this status reason]
    (.close this status reason))
  (connected? [this]
    (.isOpen this))
  (unwrap [this]
    this))


(defn- get-headers
  "Creates a name/value map of all the request headers."
  [^HttpServletRequest request]
  (reduce
    (fn [headers, ^String name]
      (assoc headers
        (.toLowerCase name Locale/ENGLISH)
        (->> (.getHeaders request name)
             (enumeration-seq)
             (string/join ","))))
    {}
    (enumeration-seq (.getHeaderNames request))))

(defn- get-client-cert
  "Returns the SSL client certificate of the request, if one exists."
  [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn build-request-map
  "Create the request map from the HttpServletRequest object."
  [^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request) Locale/ENGLISH))
   :protocol           (.getProtocol request)
   :headers            (get-headers request)
   :ssl-client-cert    (get-client-cert request)})

(defn set-headers [^ServletUpgradeResponse response headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Some headers must be set through specific methods
  (when-let [protocol (get headers ServletUpgradeResponse/SEC_WEBSOCKET_PROTOCOL)]
    (.setAcceptedSubProtocol response protocol)))

(defn update-servlet-response
  "Copy fields from a response map to the ServletUpgradeResponse object"
  [^ServletUpgradeResponse res {:keys [status headers]}]
  (when status
    (.setStatus res status))
  (when headers
    (set-headers res headers)))

(defn- noop [& _args])

(defn- create-ws [{:keys [on-connect on-error on-text on-close on-binary]
                  :or {on-connect noop
                       on-close noop
                       on-error noop
                       on-text noop
                       on-binary noop}}]
  (Listener. on-connect on-close on-error on-text on-binary))

(defn- create-ws-creator [handshake-handler]
  (reify WebSocketCreator
   (createWebSocket [_this req resp]
     (let [req-map (build-request-map (.getHttpServletRequest req))
           handshake-result (handshake-handler req-map)]
       (update-servlet-response resp handshake-result)
       (if-let [handlers (:ws-handlers handshake-result)]
         (create-ws handlers)
         (.sendError resp (.getStatusCode resp) ""))))))

(defn- create-ws-handler [handshake-handler]
  "Creates a handler for WebSocket upgrade requests."
  (proxy [WebSocketHandler] []
    (configure [^WebSocketServletFactory factory]
      ; can configure timeout (and maybe other options) here
     (.setCreator factory (create-ws-creator handshake-handler)))
    (handle [target ^Request base-request request response]
      ;If this is a WS request, always mark it as handled.
      (when (.isUpgradeRequest (proxy-super getWebSocketFactory) request response)
        (proxy-super handle target base-request request response)
        (.setHandled base-request true)))))

(defn configurator
  "Takes a handshake handler function and returns a server configurator that will
  add ring-like WebSocket handling to a jetty server. The handshake-handler is much
  like a ring HTTP request handler, and all upgrade requests will be directed to it.
  In order to establish a WebSocket connection, the handler should return a map with
  a status of 101. In addition a map of websocket event handler functions can be
  provided in the :ws-handlers key, as in the example below.

      {:status 101
       :ws-handlers {:on-connect (fn [ws]
                       (log/debug \"WebSocket connected\" ws))
                     :on-close (fn [ws code reason]
                       (log/debug \"Websocket closed\" ws code reason))
                     :on-error (fn [ws err]
                       (log/error err \"Websocket connection error!\" ws))
                     :on-text (fn [ws text]
                       (log/debug \"Websocket text recieved\" ws text))
                     :on-binary (fn [ws bytes offset len]
                       (log/debug \"Websocket binary data recived\" len)}}

  The first argument to each handler will be an object fulfilling the WebSocket protocol"
  [handshake-handler]
  (fn [^Server server]
    (let [orig-handler (.getHandler server)
          ws-handler (create-ws-handler handshake-handler)
          combined-handler (doto (HandlerList.)
                             (.addHandler ws-handler)
                             (.addHandler orig-handler))]
      (.setHandler server combined-handler))))
