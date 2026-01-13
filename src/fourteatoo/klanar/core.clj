(ns fourteatoo.klanar.core
  (:require [fourteatoo.klanar.mail :as mail]
            [fourteatoo.klanar.imap :as imap]
            [fourteatoo.klanar.conf :as c :refer [conf opt]]
            [fourteatoo.klanar.log :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [fourteatoo.klanar.http :as http]
            [mount.core :as mount]
            [clojure.java.io :as io])
  (:gen-class))

(defn- log-message [msg]
  (log/debug "msg:" (mail/get-message-id (:message msg)))
  msg)

(defn- search-renew-messages [inbox search]
  (->> (imap/folder-search inbox (merge (conf :search)
                                        search))
       (map mail/parse-message)
       (remove (comp nil? :renew-link))))

(defn- messages-move [inbox messages outbox]
  (log/debug "moving" (count messages) "messages from" inbox "to" outbox)
  (imap/messages-move inbox (map :message messages) outbox))

(defn- messages-delete [inbox messages]
  (log/debug "deleting" (count messages) "messages from" inbox)
  (imap/messages-mark-as-deleted inbox (map :message messages)))

(defn- messages-mark-as-seen [inbox messages]
  (log/debug "marking as seen" (count messages) "messages from" inbox)
  (imap/messages-mark-as-seen inbox (map :message messages)))

(defn- ensure-folder [store name]
  (let [folder (imap/get-folder imap/store (conf :after-processing :move-to))]
    (when-not (imap/folder-exists? folder)
      (imap/folder-create folder))
    folder))

(defn- process-messages [f & [search]]
  (log/info "process messages" (str search))
  (with-open [inbox (imap/open-folder imap/store (or (conf :imap :folder)
                                                     "INBOX"))]
    (let [messages (search-renew-messages inbox search)
          outbox (when (conf :after-processing :move-to)
                   (ensure-folder imap/store (conf :after-processing :move-to)))]
      (log/info "processing" (count messages) "messages")
      (->> messages
           (map log-message)
           (run! f))
      (when-not (opt :dry-run)
        (when (conf :after-processing :mark-as-seen)
          (messages-mark-as-seen inbox messages))
        ;; either move or delete; not both
        (cond (conf :after-processing :move-to)
              (messages-move inbox messages outbox)
              (conf :after-processing :delete)
              (messages-delete inbox messages))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private cli-options
  [["-c" "--config FILE" "Confirguration file"
    :parse-fn #(io/file %)
    :validate [#(.exists %) "Configuration file does not exist"]]
   ["-n" "--dry-run" "Don't actually renew ads"]
   ["-l" "--list-folders" "List all folders on the server"]
   ["-v" "--verbose" "Increase logging verbosity"
    :default 0
    :update-fn inc]
   ["-h" "--help" "Show program usage"]])

(defn- usage [summary errors]
  (doseq [e errors]
    (println e))
  (println "usage: klanar [options ...]")
  (when summary
    (println summary))
  (System/exit -1))

(defn- parse-cli [args]
  (let [{:keys [arguments summary errors] :as result} (parse-opts args cli-options)]
    (when (or errors
              (seq arguments))
      (usage summary errors))
    result))

(defn- renew-ads []
  (process-messages (fn [msg]
                      (log/info "ad" (get-in msg [:ad :title]) "is expiring"
                                (str "(" (get-in msg [:ad :href]) ")"))
                      (when-not (opt :dry-run)
                        (http/extend-ad (:renew-link msg))))))

(defn- list-folders []
  (run! (comp println imap/folder-full-name)
        (imap/list-folders imap/store)))

(defn -main [& args]
  (let [{:keys [options summary]} (parse-cli args)]
    (binding [c/options options]
      (mount/start)
      (cond (:help options)
            (usage summary nil)
            (:list-folders options)
            (list-folders)
            :else
            (renew-ads)))))
