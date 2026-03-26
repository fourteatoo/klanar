(defproject io.github.fourteatoo/klanar "0.1.0-SNAPSHOT"
  :description "KleinAnzeige Automatic Renew"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.clojure/tools.logging "1.3.0"]
                 [clojure.java-time "1.4.3"]
                 [spootnik/unilog "0.7.32"]
                 [com.sun.mail/jakarta.mail "2.0.2"]
                 [org.clj-commons/hickory "0.7.7"]
                 [org.clojure/tools.cli "1.2.245"]
                 [diehard "0.12.0"]
                 [clojure.java-time "1.4.3"]
                 [cprop "0.1.19"]
                 [hato "1.0.0"]
                 [camel-snake-kebab "0.4.3"]
                 [nrepl "1.5.1"]
                 [mount "0.1.23"]]
  :main ^:skip-aot fourteatoo.klanar.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:plugins [[lein-codox "0.10.8"]
                             [lein-cloverage "1.2.4"]]
                   :resource-paths ["dev-resources" "resources"]}})
