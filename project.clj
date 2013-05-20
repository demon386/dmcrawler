(defproject dmcrawler "0.1.0-SNAPSHOT"
  :description "Crawler for Douban Movie short comments."
  :url "http://github.com/demon386/dmcrawler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.1"]
                 [korma "0.3.0-RC5"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [clj-time "0.5.0"]]
  :main dmcrawler.core)
