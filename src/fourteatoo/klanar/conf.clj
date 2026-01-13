(ns fourteatoo.klanar.conf
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cprop.core :as cprop]
   [mount.core :as mount]))

(def ^:dynamic options
  "Command line options map as returned by
  `clojure.tools.cli/parse-opts`.  This should be dynamically bound in
  the main function just after parsing the command line."
  nil)

(defn opt [o]
  (get options o))

(defn- home-dir []
  (System/getProperty "user.home"))

(defn- home-conf []
  (io/file (home-dir) ".klanar"))

(defn- load-configuration [& [file]]
  (let [file (io/as-file (or file (home-conf)))]
    (if (and file (.exists file))
      (cprop/load-config :file (str file))
      (cprop/load-config))))

(mount/defstate config
  :start (load-configuration (opt :config)))

(defn conf [& path]
  (get-in config path))

