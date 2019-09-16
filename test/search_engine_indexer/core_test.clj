(ns search-engine-indexer.core-test
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [search-engine-indexer.core :refer :all]
            [search-engine-indexer.utils :refer :all]))

(deftest end-to-end-test
  (testing "End-to-end generation and processing of search term log files"
    (let [dictionary-file "test/resources/dictionary.txt"
          search-term-log-files-directory (str "/tmp/search_engine_indexer/"
                                               (rand-int 1000))
          occurrence-counts-directory (str search-term-log-files-directory
                                           "_occurrence_counts")
          sorted-search-terms-log-file (str search-term-log-files-directory ".log")
          human-bytes-to-write "10MB"
          number-of-output-files 3
          maximum-terms-in-memory 10000
          bytes-to-write (human->bytes human-bytes-to-write)
          number-of-search-terms-in-dictionary (-> dictionary-file
                                                   (slurp)
                                                   (s/split #"\n")
                                                   (count))]
      (remove-directory (io/file search-term-log-files-directory))
      (-main "generate"
             "--dictionary-file" dictionary-file
             "--number-of-output-files" (str number-of-output-files)
             "--human-bytes-to-write" human-bytes-to-write
             "--output-directory" search-term-log-files-directory)
      (is (= number-of-output-files
             (count (.listFiles (io/file search-term-log-files-directory))))
          "creates the requested amount of output files")
      (is (<= bytes-to-write
              (directory-size (io/file search-term-log-files-directory)))
          "creates files totaling the requested total size")
      (-main "process"
             "--input-directory" search-term-log-files-directory
             "--output-file" sorted-search-terms-log-file
             "--maximum-terms-in-memory" (str maximum-terms-in-memory))
      (is (= number-of-search-terms-in-dictionary
             (count (.listFiles (io/file occurrence-counts-directory))))
          "creates one occurrence count file per search term")
      (let [sorted-search-terms (-> sorted-search-terms-log-file
                                    (slurp)
                                    (s/split #"\n"))]
        (is (= (sort sorted-search-terms)
               sorted-search-terms)
            "creates an alphabetically sorted search term log file")))))
