(ns search-engine-indexer.generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [search-engine-indexer.utils :refer [lazy-file-seq runtime]]))

(def units
  "TODO: docstring."
  {:b 1
   :kb 1000
   :mb (Math/pow 1000 2)
   :gb (Math/pow 1000 3)
   :tb (Math/pow 1000 4)
   :kib 1024
   :mib (Math/pow 1024 2)
   :gib (Math/pow 1024 3)
   :tib (Math/pow 1024 4)})

(def human-regex
  "TODO: docstring."
  (let [units-regex (->> (keys units) (map name) (s/join "|") (#(str "(" % ")?")))
        size-regex "((?:\\d+\\.)?\\d+)"
        regex-string (str "(?i)" size-regex "\\s*" units-regex "\\b")]
    (re-pattern regex-string)))

(defn human->bytes
  "TODO: docstring."
  [human]
  (let [[match? number-string unit-string] (re-find human-regex human)
        number (edn/read-string number-string)
        unit (and unit-string (keyword (s/lower-case unit-string)))]
    (assert match? (str "'" human "' isn't a valid unit pattern"))
    (* number (or (get units unit) 1))))

(defn bytes->human
  "TODO: docstring."
  [bytes* {:keys [in]}]
  (let [unit (keyword (s/lower-case (name in)))]
    (assert
     (contains? units unit)
     (str "'" in "'" " isn't a valid unit. Valid units are: " (keys units)))
    (* bytes* (get units unit))))

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
            _ (.mkdir (io/file output-directory))
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

(comment
  (let [size "0.5GB"]
    (generate-random-search-term-files (io/resource "dictionary.txt")
                                       15
                                       size
                                       (str "search-terms-" size))))
