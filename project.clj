(defproject inin-purecloud/ring-jetty-ws "2.0.0-SNAPSHOT"
  :description ""
  :url "https://github.com/MyPureCloud/ring-jetty-ws"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :aot [ring-jetty-ws.listener]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ org.eclipse.jetty.websocket/websocket-server "9.4.11.v20180605"]]
  :profiles {
    :provided {
      :dependencies [[ring/ring-jetty-adapter "1.6.0"]]}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
