(ns search-engine-indexer.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [search-engine-indexer.utils
             :refer [files-equal? lazy-file-seq runtime truncate-file]]))

(defn process
  "TODO: docstring."
  [input-directory output-file]
  (let [occurrence-counts (atom (sorted-map))
        input-files (.listFiles (io/file input-directory))
        _ (println "Processing files in"
                   (str "'" (.getAbsolutePath (io/file input-directory)) "'"))
        _ (doseq [file-name input-files
                  :let [file (io/file file-name)]]
            (println (str "'" (.getAbsolutePath file) "'")
                     (str "(" (.length file) " bytes)")))
        {:keys [runtime-s]}
        (runtime
         ;; For each input file.
         (doseq [input-file input-files]
           ;; For each line (search term).
           (doseq [term (lazy-file-seq input-file)]
             ;; Increment sorted hashmap entry for term.
             (swap! occurrence-counts update term (fnil inc 0))))
         (println "Term occurrences across all files in"
                  (str "'" (.getAbsolutePath (io/file input-directory)) "'"))
         (println (s/trim-newline (with-out-str (pprint @occurrence-counts))))
         ;; Truncate output file if it exists.
         (truncate-file output-file)
         (println "Writing sorted search terms to"
                  (str "'" (.getAbsolutePath (io/file output-file)) "'"))
         (with-open [w (io/writer output-file :append true)]
           ;; For each key/value in the sorted hashmap.
           (doseq [[term occurrence-count] @occurrence-counts]
             ;; Write the term to the output file N times.
             (dotimes [_ occurrence-count]
               (.write w (str term "\n"))))))
        bytes-processed (->> input-files
                             (map (memfn length))
                             (reduce +))]
    (println bytes-processed "bytes processed in"
             (format "%.2f" runtime-s) "seconds"
             (str "("
                  (format "%.2f" (/ (/ bytes-processed 1000000) runtime-s))
                  " MB/s"
                  ")"))))

(comment
  (let [input-directory (io/resource "input")
        output-file "output.log"
        expected-output-file-resource (io/resource "expected_output.log")]
    (process input-directory output-file)
    (files-equal? output-file expected-output-file-resource))

  (let [size "5MiB"
        input-directory (str "search-terms-" size)
        output-file (str input-directory ".log")]
    (process input-directory output-file)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
