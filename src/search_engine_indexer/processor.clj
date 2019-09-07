(ns search-engine-indexer.processor
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [search-engine-indexer.utils
             :refer [lazy-file-seq runtime truncate-file]]))

(defn process-directory
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
         (doseq [input-file input-files]
           (doseq [term (lazy-file-seq input-file)]
             (swap! occurrence-counts update term (fnil inc 0))))
         (println "Term occurrences across all files in"
                  (str "'" (.getAbsolutePath (io/file input-directory)) "'"))
         (println (s/trim-newline (with-out-str (pprint @occurrence-counts))))
         (truncate-file output-file)
         (println "Writing sorted search terms to"
                  (str "'" (.getAbsolutePath (io/file output-file)) "'"))
         (with-open [w (io/writer output-file :append true)]
           (doseq [[term occurrence-count] @occurrence-counts]
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
