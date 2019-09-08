(ns search-engine-indexer.utils
  (:require [clojure.java.io :as io])
  (:import java.util.Properties))

(defn jar-path
  "Returns the full path of the JAR file in which this function is invoked."
  [& [ns]]
  (-> (or ns (class *ns*))
      (.getProtectionDomain)
      (.getCodeSource)
      (.getLocation)
      (.toURI)
      (.getPath)))

(defn version
  "Returns the version number for a given x or namespace."
  [x]
  (when-let [properties (io/resource (str "META-INF/maven"
                                          "/" (or (namespace x) (name x))
                                          "/" (name x)
                                          "/" "pom.properties"))]
    (with-open [stream (io/input-stream properties)]
      (.getProperty (doto (Properties.) (.load stream)) "version"))))

(defmacro runtime
  "Returns a map with the return value from body and its runtime information."
  [& body]
  `(let [start# (. System (nanoTime))
         result# (do ~@body)
         runtime-ms# (/ (- (. System (nanoTime))
                           start#)
                        1000000.0)]
     {:runtime-ms runtime-ms#
      :runtime-s (/ runtime-ms# 1000)
      :value result#}))

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
