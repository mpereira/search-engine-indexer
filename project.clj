(defproject search-engine-indexer "0.2.0-SNAPSHOT"
  :description "search-engine-indexer is a tool for working with search term log files"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.apache.commons/commons-io "1.3.2"]]
  :main ^:skip-aot search-engine-indexer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[criterium "0.4.5"]]
                   :resource-paths ["test/resources"]
                   :plugins [[lein-ancient "0.6.15"]
                             [lein-bikeshed "0.5.2"]
                             [lein-cljfmt "0.6.4"]]}})
