(ns search-engine-indexer.generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [search-engine-indexer.utils :refer [human->bytes
                                                 lazy-file-seq
                                                 make-directory
                                                 runtime]]))

(defn generate-random-search-term-files
  "TODO: docstring."
  [dictionary-file number-of-files human-bytes-to-write output-directory]
  (if (.exists (io/file output-directory))
    (println output-directory "already exists")
    (if-not (.exists (io/file dictionary-file))
      (println "Dictionary file" dictionary-file "doesn't exist")
      (let [bytes-written (atom 0)
            bytes-to-write (human->bytes human-bytes-to-write)
            default-term-batch-size 10
            terms (vec (lazy-file-seq dictionary-file))
            number-of-terms (count terms)
            _ (println "Read dictionary file with" number-of-terms "terms")
            _ (make-directory (io/file output-directory))
            output-directory-file (io/file output-directory)
            _ (println "Created output directory"
                       (str "'" (.getAbsolutePath output-directory-file) "'"))
            output-file-names (map #(str (.getAbsolutePath output-directory-file)
                                         "/" % ".log")
                                   (range number-of-files))
            writers (vec (map #(io/writer % :append true) output-file-names))
            number-of-writers (count writers)]
        (println "Writing" human-bytes-to-write
                 "across" number-of-writers "output files")
        (let [{:keys [runtime-s]}
              (runtime
               (while (< @bytes-written bytes-to-write)
                 (let [w (get writers (rand-int number-of-writers))
                       term-generator (fn []
                                        (repeatedly
                                         #(get terms (rand-int number-of-terms))))
                       term-batch (str (->> (term-generator)
                                            (take default-term-batch-size)
                                            (s/join "\n"))
                                       "\n")
                       term-batch-size-in-bytes (count term-batch)]
                   (.write w term-batch)
                   (swap! bytes-written + term-batch-size-in-bytes))))]
          (doseq [w writers]
            (.close w))
          (doseq [file-name output-file-names
                  :let [file (io/file file-name)]]
            (println "Created" (str "'" (.getAbsolutePath file) "'")
                     (str "(" (.length file) " bytes)")))
          (println @bytes-written "bytes written in"
                   (format "%.2f" runtime-s) "seconds"
                   (str "("
                        (format "%.2f" (/ (/ @bytes-written 1000000) runtime-s))
                        " MB/s"
                        ")")))))))
