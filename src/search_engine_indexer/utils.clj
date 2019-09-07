(ns search-engine-indexer.utils
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]))

(defmacro runtime [& body]
  `(let [start# (. System (nanoTime))
         result# (do ~@body)
         runtime-ms# (/ (- (. System (nanoTime))
                           start#)
                        1000000.0)]
     {:runtime-ms runtime-ms#
      :runtime-s (/ runtime-ms# 1000)
      :value result#}))

(defn pprinted-string [x]
  (with-out-str (pprint x))

(defn lazy-file-seq
  "Create a lazy seq of lines from given file."
  [filename]
  (letfn [(helper [r]
            (lazy-seq (if-let [line (.readLine r)]
                        (do (cons line (helper r)))
                        (do (.close r) nil))))]
    (helper (io/reader filename))))

(defn truncate-file
  "Truncates file to zero bytes."
  [file]
  (with-open [chan (.getChannel (java.io.FileOutputStream. file true))]
    (.truncate chan 0)))

(defn files-equal?
  "Returns true if two files have the same content, false otherwise."
  [f1 f2]
  (let [f1-seq (lazy-file-seq f1)
        f2-seq (lazy-file-seq f2)
        combined-seq (map vector f1-seq f2-seq)]
    (reduce (fn [found-different-terms? [f1-term f2-term]]
              (if (= f1-term f2-term)
                found-different-terms?
                (reduced false)))
            true
            combined-seq)))
