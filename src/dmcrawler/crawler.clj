(ns dmcrawler.crawler
  (:require [net.cgrand.enlive-html :as html]))


(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn- extract-ids
  "Extract a seq of ids from comments info.
   It's extracted from the link of user's movie page."
  [comments-info-nodes]
  (letfn [(extractor [x]
            (->> ((comp :href :attrs second :content) x)
                 (re-find #"http://movie.douban.com/people/(.*)/")
                 (second)))]
    (map extractor comments-info-nodes)))

(defn- extract-dates
  "The position of content depends on whether the user has rating."
  [comments-info-html]
  (letfn [(extractor [x]
            (let [content (:content x)]
              (clojure.string/trim (if (< (count content) 5)
                                     (nth content 2)
                                     (nth content 4)))))]
    (map extractor comments-info-html)))

(defn- extract-rates
  "The raw string for rates are something like allstar10, allstar20, allstar30, etc.
   This function will extract the raw string and further extract the rating as an Integer."
  [comments-info-html]
  (letfn [(extractor [x]
            (let [content (:content x)]
              (if (< (count content) 5)
                nil
                (-> content
                    (nth 3) :attrs :class
                    (clojure.string/split #" ")
                    first
                    (#(re-find #"allstar(\d)0" %))
                    second
                    (Integer.)))))]
    (map extractor comments-info-html)))

(defn extract-comments-content [x]
  (map (comp first :content) x))


(defn fetch-users-rates-info-in-url
  "Extract (id, date, rate, comment) from the url.
   ids, dates, and rates are in .comment-info nodes,
   while comments are in .comment > p nodes."
  [url]
  (let [html-res (fetch-url url)
        comments-content-nodes (html/select html-res [:.comment :> :p])
        comments-info-nodes (html/select html-res [:.comment-info])
        ids (extract-ids comments-info-nodes)
        dates (extract-dates comments-info-nodes)
        rates (extract-rates comments-info-nodes)
        comments-content (extract-comments-content comments-content-nodes)]
    (when-not (= `(~@(map count (list ids dates rates comments-content))))
      (throw (Exception. "The numbers of different fields don't match!")))
    (partition 4 (interleave ids dates rates comments-content))))


(defn fetch-num-of-rates [url]
  (let [html (fetch-url url)
        total-num-html (html/select html [:.title_line :> :.fleft])]
    (->> (first total-num-html)
         :content
         first
         (re-find #"全部共(\d+)条")
         second
         Integer.)))


(defn fetch-other-movies-in-url
  "Get other movies in the page.
   It's based on recommended movies."
  [url]
  (let [html (fetch-url url)
        other-movies-html (html/select html [:.recommendations-bd [:a (html/attr? :href)]])]
    (map (comp #(Integer. %)
               last first
               #(re-seq #"http://movie.douban.com/subject/(\d+)[/]*" %)
               :href :attrs)
       other-movies-html)))
