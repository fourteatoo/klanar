(ns fourteatoo.klanar.mail
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [hickory.core :as html]
            [hickory.select :as hsel])
  (:import [java.util Properties]
           [jakarta.mail.internet MimeMessage]
           [jakarta.mail Session Folder Flags Flags$Flag AuthenticationFailedException]))

(defn file->message
  [file]
  (let [props (Session/getDefaultInstance (Properties.))]
    (with-open [msg (io/input-stream file)]
      (MimeMessage. props msg))))

(defn get-body-part [mime-multi-part i]
  (.getBodyPart mime-multi-part i))

(defn mmp-count [mmp]
  (.getCount mmp))

(defn content-type [content]
  (.getContentType content))

(defn multipart?
  [content]
  (instance? jakarta.mail.internet.MimeMultipart content))

(defn get-content [msg]
  (.getContent msg))

(defn message-headers [msg]
  (->> (.getAllHeaders msg)
       enumeration-seq
       (reduce (fn [m h]
                 (assoc m
                        (.getName h)
                        (.getValue h)))
               {})))

(defmulti convert-content class)

(defmethod convert-content jakarta.mail.internet.MimeMessage
  [c]
  {:headers (message-headers c)
   :content-type (content-type c)
   :body (convert-content (get-content c))
   :message c})

(defmethod convert-content jakarta.mail.internet.MimeMultipart
  [c]
  {:content-type (content-type c)
   :body (->> (range (mmp-count c))
              (map (partial get-body-part c))
              (map convert-content)
              ;; the mbox could bec losed before this is fully
              ;; realized
              doall)})

(defmethod convert-content String
  [c]
  c)

(defmethod convert-content :default
  [c]
  {:content-type (content-type c)
   :body (get-content c)})

(defn html-content? [c]
  (and (:content-type c)
       (s/starts-with? (s/lower-case (:content-type c)) "text/html")))

(defn find-mime-parts [f? c]
  (cond (f? c) [c]

        (seq? (:body c))
        (->> (:body c)
             (mapcat (partial find-mime-parts f?)))

        :else nil))

(defn find-html-parts [c]
  (find-mime-parts html-content? c))

(defn select-action-button [html]
  (hsel/select (hsel/child (hsel/tag :table)
                           (hsel/tag :tbody)
                           (hsel/tag :tr)
                           (hsel/tag :td)
                           (hsel/and (hsel/tag :a)
                                     (hsel/attr :title (partial = "Anzeige verlängern"))
                                     #_(hsel/class "t_c0c0c0b")))
               html))

(defn select-ad-link [html]
  (hsel/select (hsel/descendant (hsel/tag :td)
                                (hsel/find-in-text #"Deine Anzeige +")
                                (hsel/tag :a))
               html))

(defn extract-body-from-message [msg]
  (convert-content msg))

(defn- extract-hickory [c]
  (->> (find-html-parts c)
       (map (fn [p]
              (assoc p :html (html/parse (:body p)))))
       (map (fn [p]
              (assoc p :hickory (html/as-hickory (:html p)))))))

(defn- extract-action-url-from-content [parts]
  (->> parts
       (mapcat (fn [p]
                 (select-action-button (:hickory p))))
       (remove nil?)
       (map #(get-in % [:attrs :href]))
       first))

(defn- extract-ad-url-from-content [parts]
  (->> parts
       (mapcat (comp select-ad-link :hickory))
       (remove nil?)
       (map #(select-keys (:attrs %) [:href :title]))
       first))

(defn parse-message [msg]
  (let [msg (convert-content msg)
        hickory-parts (extract-hickory msg)]
    (-> msg
        (assoc :renew-link (extract-action-url-from-content hickory-parts))
        (assoc :ad (extract-ad-url-from-content hickory-parts)))))

(defn message-id [msg]
  (.getMessageID msg))

(defn message-subject [msg]
  (.getSubject msg))

(defn message-sender [msg]
  (.getSender msg))

(defn message-send-date [msg]
  (.getSentDate msg))
