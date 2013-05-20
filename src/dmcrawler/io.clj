(ns dmcrawler.io
  (:use clojure.java.io))


;; Borrowed from http://clojuredocs.org/clojure_core/clojure.core/print-dup
(defn serialize-to [file form]
  (with-open [wtr (writer file)]
      (print-dup form wtr)))

(defn read-serialized-from
  [file]
  (with-open [r (java.io.PushbackReader. (reader file))]
     (read r)))

(defn write-to [file form]
  (with-open [wtr (writer file)]
    (.write wtr form)))
