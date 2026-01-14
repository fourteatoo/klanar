(ns fourteatoo.klanar.imap
  (:require
   [clojure.java.io :as io]
   [fourteatoo.klanar.conf :as c :refer [conf]]
   [mount.core :as mount])
  (:import
   (jakarta.mail Flags Flags$Flag Folder Message Message$RecipientType Session)
   (jakarta.mail.event MessageCountListener)
   (jakarta.mail.search AndTerm ComparisonTerm FlagTerm FromStringTerm ReceivedDateTerm RecipientStringTerm SubjectTerm)
   (java.util Calendar Properties)))

(defn days-ago [n]
  (let [cal (Calendar/getInstance)]
    (.add cal Calendar/DAY_OF_MONTH (- n))
    (.getTime cal)))

(defn map->properties [m]
  (doto (java.util.Properties.)
    (.putAll m)))

(defn- make-imap-session [session-parms]
  (let [props (map->properties
               (merge {"mail.store.protocol" "imaps"
                       "mail.imaps.port" 993
                       "mail.imaps.ssl.enable" true}
                      session-parms))]
    (Session/getInstance props)))

(defn connect-store
  [session email app-password]
  (doto (.getStore session "imaps")
    (.connect nil email app-password)))

(defn open-default-folder [store & {:keys [mode]
                                    :or {mode Folder/READ_WRITE}}]
  ;; store.getDefaultFolder().list();
  (doto (.getDefaultFolder store)
    (.open mode)))

(defn folder-full-name [folder]
  (.getFullName folder))

(defn folder-name [folder]
  (.getName folder))

(defn list-folders [store]
  (.. store getDefaultFolder (list "*")))

(defn get-folder [store folder-name]
  (.getFolder store folder-name))

(defn as-folder [store folder]
  (if (instance? Folder folder)
    folder
    (get-folder store folder)))

(defn folder-exists? [folder]
  (.exists folder))

(defn folder-create [folder & [type]]
  (.create folder (or type Folder/HOLDS_MESSAGES)))

(defn folder-delete [folder & [recursive?]]
  (.delete folder (boolean recursive?)))

(defn open-folder [store folder & {:keys [mode]
                                   :or {mode Folder/READ_WRITE}}]
  (doto (as-folder store folder)
    (.open mode)))

(defn folder-open? [folder]
  (.isOpen folder))

(defn- build-search-term
  [{:keys [days from to subject unread]
    :or   {days 2 unread true}}]
  (let [terms (cond-> [(ReceivedDateTerm.
                        ComparisonTerm/GE
                        (days-ago days))]

                unread
                (conj
                 (FlagTerm.
                  (Flags. Flags$Flag/SEEN)
                  false))

                from
                (conj (FromStringTerm. from))

                to
                (conj (RecipientStringTerm. Message$RecipientType/TO to))

                subject
                (conj (SubjectTerm. subject)))]
    (reduce (fn [a b]
              (AndTerm. a b))
            terms)))

(defn folder-search [folder search-parms]
  (seq (.search folder (build-search-term search-parms))))

(defn folder-set-flag
  ([folder messages flag]
   (folder-set-flag folder messages flag true))
  ([folder messages flag set?]
   (.setFlags folder (into-array Message messages) (Flags. flag) set?)))

(defn folder-unset-flag [folder messages flag]
  (folder-set-flag folder messages flag false))

(defn message-folder [msg]
  (.getFolder msg))

(defn message-set-flag
  ([msg flag]
   (message-set-flag msg flag true))
  ([msg flag set?]
   (.setFlag msg flag set?)))

(defn message-mark-as-seen [msg]
  (message-set-flag Flags$Flag/SEEN))

(defn message-mark-as-deleted [msg]
  (message-set-flag Flags$Flag/DELETED))

(defn folder-store [folder]
  (.getStore folder))

(defn message-store [msg]
  (folder-store (message-folder msg)))

(defn message-copy [msg dest]
  (.copyMessages (message-folder msg) (into-array Message [msg]) (as-folder (message-store msg) dest)))

(defn message-move [msg dest]
  (.moveMessages (message-folder msg) (into-array Message [msg]) (as-folder (message-store msg) dest)))

(defn messages-mark-as-seen [folder messages]
  (folder-set-flag folder messages Flags$Flag/SEEN))

(defn messages-mark-as-deleted [folder messages]
  (folder-set-flag folder messages Flags$Flag/DELETED))

(defn messages-copy [in-folder messages out-folder]
  (.copyMessages in-folder (into-array Message messages) out-folder))

(defn messages-move [in-folder messages out-folder]
  (.moveMessages in-folder (into-array Message messages) out-folder))

(defn add-message-count-listener [folder & {:keys [added removed]
                                            :or {added identity
                                                 removed identity}}]
  (.addMessageCountListener folder
                            (reify MessageCountListener
                              (messagesAdded [_ e]
                                (added e))
                              (messagesRemoved [_ e]
                                (removed e)))))

(defn event-messages [e]
  (.getMessages e))

(mount/defstate session
  :start (make-imap-session (conf :imap :session)))

(mount/defstate store
  :start (connect-store session (conf :imap :user) (conf :imap :password))
  :stop (.close store))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn save-message [msg file]
  (with-open [out (io/output-stream file)]
    (.writeTo msg out))
  file)
