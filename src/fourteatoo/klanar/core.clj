(ns fourteatoo.klanar.core
  (:require [fourteatoo.klanar.mail :as mail]
            [fourteatoo.klanar.imap :as imap]
            [fourteatoo.klanar.conf :as c :refer [conf opt]]
            [fourteatoo.klanar.log :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [fourteatoo.klanar.http :as http]
            [mount.core :as mount]
            [clojure.java.io :as io]
            [fourteatoo.klanar.misc :as misc]
            [diehard.core :as dh])
  (:gen-class))


(defn- filter-renewals [messages]
  (remove (comp nil? :renew-link) messages))

(defn- convert-messages [messages]
  (map mail/parse-message messages))

(defn- search-renew-messages [inbox search]
  (->> (imap/folder-search inbox (merge (conf :search)
                                        search))
       convert-messages
       filter-renewals))

(defn- messages-move [inbox messages outbox]
  (log/debug "moving" (count messages) "messages from"
             (imap/folder-full-name inbox) "to"
             (imap/folder-full-name outbox))
  (imap/messages-move inbox (map :message messages) outbox))

(defn- messages-delete [inbox messages]
  (log/debug "deleting" (count messages) "messages from"
             (imap/folder-full-name inbox))
  (imap/messages-mark-as-deleted inbox (map :message messages)))

(defn- messages-mark-as-seen [inbox messages]
  (log/debug "marking as seen" (count messages) "messages from"
             (imap/folder-full-name inbox))
  (imap/messages-mark-as-seen inbox (map :message messages)))

(defn- ensure-folder [folder]
  (let [folder (imap/as-folder imap/store folder)]
    (when-not (imap/folder-exists? folder)
      (imap/folder-create folder))
    folder))

(defn- inbox-name []
  (or (conf :imap :folder)
      "INBOX"))

(defn- open-inbox []
  (imap/open-folder imap/store (inbox-name)))

(defn- dispose-of-processed-messages [inbox messages]
  (when-not (opt :dry-run)
    (log/debug "disposing of" (count messages) "messages")
    (let [outbox (when (conf :after-processing :move-to)
                   (ensure-folder (conf :after-processing :move-to)))]
      (when (conf :after-processing :mark-as-seen)
        (messages-mark-as-seen inbox messages))
      ;; either move or delete
      (cond (conf :after-processing :move-to)
            (messages-move inbox messages outbox)
            (conf :after-processing :delete)
            (messages-delete inbox messages)))))

(defn- process-batch [messages inbox]
  (log/info "processing" (count messages) "messages")
  (doseq [msg messages]
    (log/debug "msg:" (mail/message-id (:message msg)))
    (log/info "ad" (get-in msg [:ad :title]) "is expiring"
              (str "(" (get-in msg [:ad :href]) ")"))
    (when-not (opt :dry-run)
      (http/extend-ad (:renew-link msg))))
  (dispose-of-processed-messages inbox messages))

(defn- process-last-period [& [search]]
  (log/info "process messages" (str search))
  (with-open [inbox (open-inbox)]
    (let [messages (search-renew-messages inbox search)]
      (process-batch messages inbox))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- file-exists? [f]
  (.exists f))

(def ^:private cli-options
  [["-c" "--config FILE" "use confirguration file"
    :parse-fn io/file
    :validate [file-exists? "Configuration file does not exist"]]
   ["-d" "--days N" "process messages from the last N days"
    :parse-fn #(Integer/parseUnsignedInt %)]
   ["-n" "--dry-run" "don't actually renew ads"]
   ["-m" "--monitor" "enter monitor mode"]
   ["-l" "--list-folders" "list all folders on the server"]
   ["-v" "--verbose" "increase logging verbosity"
    :default 0
    :update-fn inc]
   ["-h" "--help" "show program usage"]])

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
  (process-last-period {:days (or (opt :days) 7)}))

(defn- list-folders []
  (run! (comp println imap/folder-full-name)
        (imap/list-folders imap/store)))

(defn- extract-message-info [msg]
  {:id (mail/message-id msg)
   :subject (mail/message-subject msg)
   :from (mail/message-sender msg)
   :date (mail/message-send-date msg)})

(defn- handle-add-event [event]
  (try
    (dh/with-retry {:policy misc/retry-policy}
      (let [messages (imap/event-messages event)
            mbox (imap/message-folder (first messages))]
        (log/debug "event: got" (count messages) "adds")
        (-> messages
            convert-messages
            filter-renewals
            (process-batch mbox))))
    (catch Exception ex
      (log/error ex "handle-add-event: some messages may have been left unprocessed")
      (log/error (ex-cause ex) "failed batch:" (pr-str (map extract-message-info (imap/event-messages event)))))))

(defn- handle-del-event [e]
  (log/debug "event: got" (count (imap/event-messages e)) "deletes"))

(defn- monitor-mailbox []
  (println "Entering monitor mode.\nType Ctrl-C to exit.")
  (misc/arm-exit-hooks)
  (with-open [inbox (open-inbox)]
    (log/info "listening to mailbox" (inbox-name) "events")
    (imap/add-message-count-listener inbox
                                     :added handle-add-event
                                     :removed handle-del-event)
    (loop []
      ;; the listener gets events only when we do something with the
      ;; API, so we need to check if the mailbox is open once in a
      ;; while.
      (Thread/sleep (* (or (conf :poll-period) 3) 1000))
      (imap/folder-open? inbox)
      (recur))))

(comment
  (mount/start)
  (monitor-mailbox))

(defn -main [& args]
  (let [{:keys [options summary]} (parse-cli args)]
    (binding [c/options options]
      (mount/start)
      (log/debug "DEBUGGING")
      (log/trace "TRACING")
      (cond (:help options)
            (usage summary nil)
            (:list-folders options)
            (list-folders)
            (:monitor options)
            (monitor-mailbox)
            :else
            (renew-ads))
      (mount/stop))))
