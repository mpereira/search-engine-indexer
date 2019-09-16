(ns search-engine-indexer.processor
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [search-engine-indexer.utils :refer [lazy-directory-list-seq
                                                 lazy-file-seq
                                                 make-directory
                                                 remove-directory
                                                 runtime
                                                 string->file-name
                                                 string->int
                                                 truncate-file]])
  (:import java.io.File))

(defn make-empty-buffer
  "TODO: docstring."
  [& more]
  {:total 0
   :size 0
   :buffer (sorted-map)})

(defn buffer-files-directory
  "TODO: docstring."
  [parent-directory]
  (str parent-directory "_occurrence_counts"))

(defn buffer-file
  "TODO: docstring."
  [buffer-files-directory* term]
  (str buffer-files-directory* "/" (string->file-name term)))

(defn buffer-full?
  "TODO: docstring."
  [occurrence-counts maximum-terms-in-memory]
  (>= (:size @occurrence-counts) maximum-terms-in-memory))

(defn update-buffer!
  "TODO: docstring."
  [occurrence-counts term]
  (swap! occurrence-counts (fn update-buffer [b]
                             (-> b
                                 (update :size inc)
                                 (update :total inc)
                                 (update-in [:buffer term] (fnil inc 0))))))

(defn flush-buffer!
  "TODO: docstring."
  [buffer-files-directory* occurrence-counts]
  (doseq [[term occurrence-count] (:buffer @occurrence-counts)]
    (let [term-file (buffer-file buffer-files-directory* term)]
      (with-open [w (io/writer term-file :append true)]
        (when (zero? (.length (io/file term-file)))
          (.write w (str term "\n")))
        (.write w (str occurrence-count "\n")))))
  (swap! occurrence-counts (fn flush-buffer [b]
                             (-> b
                                 (assoc :size 0)
                                 (update :buffer empty)))))

(defn aggregate-flushed-buffers!
  "TODO: docstring."
  [buffer-files-directory*]
  (doseq [buffer-file* (lazy-directory-list-seq buffer-files-directory*)]
    (let [term (first (lazy-file-seq buffer-file*))
          total-term-occurrence-count (->> buffer-file*
                                           (lazy-file-seq)
                                           (rest)
                                           (map string->int)
                                           (reduce +))]
      (with-open [w (io/writer buffer-file*)]
        (.write w (str term "\n" total-term-occurrence-count "\n"))))))

(defn process-input-files!
  "TODO: docstring."
  [input-files occurrence-counts maximum-terms-in-memory buffer-files-directory*]
  (doseq [input-file input-files]
    (doseq [term (lazy-file-seq input-file)]
      (update-buffer! occurrence-counts term)
      (when (buffer-full? occurrence-counts maximum-terms-in-memory)
        (print ".")
        (flush)
        (flush-buffer! buffer-files-directory* occurrence-counts))))
  (flush-buffer! buffer-files-directory* occurrence-counts))

(defn write-sorted-search-terms-file!
  "TODO: docstring"
  [output-file buffer-files-directory*]
  (with-open [w (io/writer output-file :append true)]
    (doseq [aggregated-buffer-file
            (sort (lazy-directory-list-seq buffer-files-directory*))]
      (let [[term term-occurrence-count-string]
            (s/split (slurp aggregated-buffer-file) #"\n")]
        (dotimes [_ (string->int term-occurrence-count-string)]
          (.write w (str term "\n")))))))

(defn print-pre-processing-report
  "TODO: docstring."
  [input-directory input-files]
  (println "Processing files in"
           (str "'" (.getAbsolutePath (io/file input-directory)) "'"))
  (doseq [^File input-file input-files]
    (println (str "'" (.getAbsolutePath input-file) "'")
             (str "(" (.length input-file) " bytes)"))))

(defn print-post-processing-report
  "TODO: docstring."
  [input-directory
   input-files
   buffer-files-directory*
   output-file runtime
   occurrence-counts]
  (println "\nTerm occurrences across all files in"
           (str "'" (.getAbsolutePath (io/file input-directory)) "'"))
  (doseq [aggregated-buffer-file
          (lazy-directory-list-seq buffer-files-directory*)]
    (let [[term term-occurrence-count-string]
          (s/split (slurp aggregated-buffer-file) #"\n")]
      (println term term-occurrence-count-string)))
  (println "Wrote sorted search terms to"
           (str "'" (.getAbsolutePath (io/file output-file)) "'"))
  (let [bytes-processed (->> input-files
                             (map (fn [^File file] (.length file)))
                             (reduce +))]
    (println bytes-processed "bytes"
             (str "(" (:total @occurrence-counts) " search terms)")
             "processed in"
             (format "%.2f" (:runtime-s runtime)) "seconds"
             (str "("
                  (format "%.2f" (/ (/ bytes-processed 1000000)
                                    (:runtime-s runtime)))
                  " MB/s"
                  ")"))))

(defn process-directory
  "TODO: docstring."
  [input-directory output-file maximum-terms-in-memory]
  (let [occurrence-counts (atom (make-empty-buffer))
        #^File lazy-input-files (lazy-directory-list-seq input-directory)
        buffer-files-directory* (buffer-files-directory input-directory)
        runtime* (runtime
                  (remove-directory (io/file buffer-files-directory*))
                  (make-directory (io/file buffer-files-directory*))
                  (print-pre-processing-report input-directory lazy-input-files)
                  (process-input-files! lazy-input-files
                                        occurrence-counts
                                        maximum-terms-in-memory
                                        buffer-files-directory*)
                  (aggregate-flushed-buffers! buffer-files-directory*)
                  (truncate-file (io/file output-file))
                  (write-sorted-search-terms-file! output-file
                                                   buffer-files-directory*))]
    (print-post-processing-report input-directory
                                  lazy-input-files
                                  buffer-files-directory*
                                  output-file
                                  runtime*
                                  occurrence-counts)))
