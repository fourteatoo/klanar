(ns fourteatoo.klanar.http
  (:require [clj-http.client :as http]
            [hickory.select :as hsel]
            [hickory.core :as html]
            [fourteatoo.klanar.log :as log]
            [clojure.string :as s]))

(defn select-result-text [html]
  (hsel/select (hsel/child (hsel/tag :div)
                           (hsel/and (hsel/tag :section)
                                     (hsel/class "outcomebox--body"))
                           (hsel/tag :p))
               html))

(defn extend-ad [url]
  (log/debug "pinging URL " url)
  (->> (http/get url {:cookie-policy :standard})
       :body
       html/parse
       html/as-hickory
       select-result-text
       (mapcat :content)
       s/join))

