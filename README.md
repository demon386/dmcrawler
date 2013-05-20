# dmcrawler (Douban Movie Crawler)
Author: Muchenxuan Tong <demon386@gmail.com>

A Clojure-based Crawler for fetching short comments from Douban movie page. (e.g. http://movie.douban.com/subject/11529526/comments?sort=time)

Features:
- Store and restore context when problems happened or the program shutted down.
- Parallelized crawling on a single machine.
- Data are stored in the database, with the time of storage information attached.

## Usage

- Set up the DB according to the comments in `src/dbcrawler/db.clj`.
- Set up the variable `starting-movie-id` in `src/dbcrawler/config.clj`. (Optional. You can use the default one.)
- With [leiningen2](https://github.com/technomancy/leiningen) installed, run the program with `lein run`.

## License

Copyright © 2013 Muchenxuan Tong

Distributed under the Eclipse Public License.
