(ns dmcrawler.core
  (:require [dmcrawler.config :as config]
            [dmcrawler.db :as db]
            [dmcrawler.crawler :as crawler]
            [dmcrawler.io :as io])
  (:gen-class))

(def current-id (atom 0))
(def visited-ids (atom (set nil)))
;; If there's no persistent states, this will be the starting movie id.
(def tovisit-ids (atom (set (list config/starting-movie-id))))
(def forbidden-ids (atom (set nil))) ; Some movie ids are forbidden for non-login guest visitor.


(defn- get-current-comment-url
  "comment url is used for fetching (user_id, date, rate, comment)."
  []
  (format "http://movie.douban.com/subject/%d/comments" @current-id))

(defn- get-current-subject-url
  "subject url is used for fetching recommended movies so that we can keep crawling new one."
  []
  (format "http://movie.douban.com/subject/%d/" @current-id))

(defn- deserialize-states []
  (try
    (do (reset! visited-ids (io/read-serialized-from "visited.txt"))
        (reset! tovisit-ids (io/read-serialized-from "tovisit.txt"))
        (reset! forbidden-ids (io/read-serialized-from "forbidden.txt")))
    (catch java.io.FileNotFoundException e
      (println "There's no serialized state. Let's start from something new."))))

(defn- serialize-states []
  (try
    (do (io/serialize-to "visited.txt" @visited-ids)
        (io/serialize-to "tovisit.txt" @tovisit-ids)
        (io/serialize-to "forbidden.txt" @forbidden-ids))
    (catch Exception e
      (.getNextException e)
      (println "Have problem saving the states.")
      (println "Please write down the following info and contact developer:")
      (println @visited-ids @tovisit-ids @forbidden-ids))))

(defn- fetch-rates-info-chunk [chunk]
  (Thread/sleep (rand-int 20000))
  (let [start (first chunk)
        end (last chunk)
        len (count chunk)
        parameter (format "?start=%d&limit=%d&sort=time" start len)
        rates-info (crawler/fetch-users-rates-info-in-url (str (get-current-comment-url) parameter))]
    (println (str (get-current-comment-url) parameter))
    (db/insert-movies-comments @current-id rates-info)))

(defn- fetch-movies-rates-info-from-current-id []
  (let [num-of-rates (crawler/fetch-num-of-rates (get-current-comment-url))
        chunks (partition-all config/chunk-size (range num-of-rates))]
    (dorun
     (pmap fetch-rates-info-chunk chunks))))

(defn- fetch-other-movies-from-current-id []
  (crawler/fetch-other-movies-in-url (get-current-subject-url)))

(defn- add-to-visit-candidates [coll]
  (println "Here's the new ids from current page: " coll)
  (let [tovisit (filter #(not (contains? (clojure.set/union @visited-ids @forbidden-ids) %)) coll)]
    (reset! tovisit-ids (clojure.set/union @tovisit-ids (set tovisit)))))

(defn- mark-current-id-as-visited []
  (swap! tovisit-ids disj @current-id)
  (swap! visited-ids conj @current-id))

(defn- process-current-id []
  (try
    (do (add-to-visit-candidates (fetch-other-movies-from-current-id))
        (fetch-movies-rates-info-from-current-id)
        (mark-current-id-as-visited)
        (println "Successfully finished visiting " @current-id))
    (catch java.io.FileNotFoundException e
      (println "This movie id is forbidden... we have no choice but to skip.")
      (swap! forbidden-ids conj @current-id)
      (mark-current-id-as-visited))
    (catch Exception e
      (println e)
      (println "I think we're going too fast.. Let's take a long sleep...")
      (println "At the same time it's always safe to save the current states...")
      (serialize-states)
      (db/clean-movie-id @current-id)
      (Thread/sleep 1000000))))


;; From http://stackoverflow.com/questions/11709639/how-to-catch-ctrlc-in-clojure
(.addShutdownHook (Runtime/getRuntime)
                  (Thread. (fn []
                             (println "Shutting down...")
                             (serialize-states)
                             (db/clean-movie-id @current-id))))


(defn- visit-a-new-id []
  (reset! current-id (first @tovisit-ids))
  (println (format "Visiting %d..." @current-id))
  (process-current-id))

(defn -main [& args]
  (deserialize-states)
  (while (not (empty? @tovisit-ids))
    (visit-a-new-id)))
