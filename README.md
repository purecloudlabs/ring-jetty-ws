# ring-jetty-ws

Provides a configurator that can be used with ring-jetty-adapter to handle websocket connections.

## Installation

Add the following to your project's `:dependencies`

```clojure
[purecloud/ring-jetty-ws 1.0.0]
```

## Usage

```clojure
(require [ring-jetty-ws.ws :as ws])

(defn ws-handler [request]
  (if (isGoodWebsocketRequest? request)
    {:status 101
     :ws-handlers {:on-connect (fn [ws]
                     (log/debug "WebSocket connected" (ws/connected? ws)))
                   :on-close (fn [ws code reason]
                     (log/debug "Websocket closed" code reason))
                   :on-error (fn [ws err]
                     (log/error err "Websocket connection error!" ws))
                   :on-text (fn [ws text]
                     (log/debug "Websocket text recieved, let's echo!")
                     (ws/send! ws text))
                   :on-binary (fn [ws bytes offset len]
                     (log/debug "Websocket binary data recived" len)}}
    {:status 400}))

(run-jetty app {:port 8080
                :configurator (ws/configurator ws-handler)}))
```

## TODO

 * Heartbeat support
 * Better binary data support
 * Tests!
 * More Docs!
