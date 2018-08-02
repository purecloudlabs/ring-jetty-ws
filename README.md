# ring-jetty-ws

## Overview

ring-jetty-ws provides ring-style handling of WebSocket connections for projects using
ring-jetty-adapter.

### What does ring-style handling mean?

Like ring, ring-jetty-ws accepts a handler function that is expected to handle all incoming requests.
The handler function is passed a request map with the same structure used in ring, and is expected
to return response maps that are compatible with those used in ring.

This means that your WebSocket handler function will be mostly compatible with ring middleware and 
other ring utilities.

### You said "mostly compatible" - what's the catch?

WebSocket requests don't include a body, so the request map will not include any body related keys:

 * :content-type
 * :content-length 
 * :character-encoding
 * :body 
 
Similarly, on the response, only :status, :headers, and :ws-handlers (see below) have meaning. Other
keys are ignored.

### What determines which requests are handled by the WebSocket handler vs the regular ring handler?

Any request for a WebSocket upgrade will be passed to the WebSocket handler, and will skip your ring handler.
For all other requests, the opposite is true.


## Installation

Add the following to your project's `:dependencies`

    [purecloud/ring-jetty-ws 1.1.0]
    
Or if you are using jetty 9.4:

    [purecloud/ring-jetty-ws 2.0.0]

## Usage

```clojure
(require '[ring-jetty-ws.ws :as ws])

(defn ws-handler [request]
  (if (isGoodWebsocketRequest? request)
    {:status 101
     ;; all handlers are optional.
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
