(ns fourteatoo.klanar.misc
  (:require
   [fourteatoo.klanar.log :as log]
   [diehard.core :as dh]
   [clojure.string :as s]
   [mount.core :as mount]))


(def exit? (promise))

(defn exiting? []
  (realized? exit?))

(defn wait-exit []
  (deref exit?))

(defn arm-exit-hooks []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (log/info "Shutting down")
                               (println "Exiting...")
                               (mount/stop)))))

(defn expand-home-dir [s]
  (s/replace-first s #"^~/" (str (System/getProperty "user.home") "/")))
