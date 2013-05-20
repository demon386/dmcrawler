(ns dmcrawler.db
  (:use korma.db
        korma.core
        clj-time.format))

; The SQL statement I used for creating the table:
; CREATE TABLE movies_comments(id SERIAL, movie_id integer NOT NULL, user_id varchar(100) NOT NULL, comment_date date NOT NULL, rate integer, comment varchar(1000) NOT NULL, store_time timestamp, PRIMARY KEY (movie_id, user_id));

; I use postgres here.
(defdb db (postgres {:db "douban" :user "demon386" :password "random"}))

(defn- parse-date-str [date-str]
  (let [date-formatter (formatter "yyyy-MM-dd")]
    (->> date-str
         (parse date-formatter)
         (.toDate)
         (.getTime)
         (java.sql.Date.))))

(defentity movies-comments
  (table :movies_comments))

(defn insert-movies-comments [movie-id coll]
  (let [store-time (java.sql.Timestamp. (.getTime (java.util.Date.)))
        insert-item (fn [[user-id comment-date rate comment-content]]
                      (try (insert movies-comments
                                   (values {:movie_id movie-id
                                            :user_id user-id :comment_date (parse-date-str comment-date)
                                            :rate rate :comment comment-content
                                            :store_time store-time}))
                           (catch Exception e (.getNextException e))))]
    (dorun (map insert-item coll))))

(defn clean-movie-id [id]
  (delete movies-comments (where {:movie_id [= id]})))
