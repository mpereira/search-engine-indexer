(ns search-engine-indexer.core
  (:require [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [search-engine-indexer.generator :as generator]
            [search-engine-indexer.processor :as processor]
            [search-engine-indexer.utils :refer [jar-path version]])
  (:gen-class))

(def program-name "search-engine-indexer")

(def program-version (version 'search-engine-indexer))

(def program-jar-path (jar-path))

(defn run-generate [{:keys [dictionary-file
                            number-of-output-files
                            human-bytes-to-write
                            output-directory]}]
  (assert (and dictionary-file
               number-of-output-files
               human-bytes-to-write
               output-directory)
          "all parameters must be present")
  (println "Dictionary file:                                            "
           dictionary-file)
  (println "Number of unsorted search term log output files:            "
           number-of-output-files)
  (println "Human-readable number bytes to write across output files:   "
           human-bytes-to-write)
  (println "Output directory for unsorted search term log output files: "
           output-directory)
  (println "")
  (generator/generate-random-search-term-files dictionary-file
                                               number-of-output-files
                                               human-bytes-to-write
                                               output-directory))

(defn run-process [{:keys [input-directory
                           output-file
                           maximum-terms-in-memory]}]
  (assert (and input-directory output-file maximum-terms-in-memory)
          "all parameters must be present")
  (println "Input directory with unsorted search term log files:          "
           input-directory)
  (println "Output file to be created with sorted search terms:           "
           output-file)
  (println "Maximum number of search terms that will be loaded in memory: "
           maximum-terms-in-memory)
  (println "")
  (processor/process-directory input-directory
                               output-file
                               maximum-terms-in-memory))

(def runners
  {:generate run-generate
   :process run-process})

(def cli-options
  {:help [["-v" "--version" "Show version"
           :id :version]
          ["-h" "--help" "Show help"
           :id :help]]
   :generate [[nil "--dictionary-file DICTIONARY_FILE"
               "File with unique terms"
               :id :dictionary-file]
              [nil "--number-of-output-files NUMBER_OF_OUTPUT_FILES"
               "Number of unsorted search term log output files. Defaults to 1"
               :default 1
               :parse-fn #(Integer/parseInt %)
               :id :number-of-output-files]
              [nil "--human-bytes-to-write HUMAN_BYTES_TO_WRITE"
               (str "Human-readable number bytes to write across output files"
                    " "
                    "E.g.: 10MB, 1GiB")
               :id :human-bytes-to-write]
              [nil "--output-directory OUTPUT_DIRECTORY"
               "Output directory for unsorted search term log output files"
               :id :output-directory]]
   :process [[nil "--input-directory INPUT_DIRECTORY"
              "Input directory with unsorted search term log files"
              :id :input-directory]
             [nil "--output-file OUTPUT_FILE"
              "Output file to be created with sorted search terms"
              :id :output-file]
             [nil "--maximum-terms-in-memory MAXIMUM_TERMS_IN_MEMORY"
              "Maximum number of search terms that will be loaded in memory"
              :parse-fn #(Integer/parseInt %)
              :id :maximum-terms-in-memory]]})

(def available-subcommands (set (keys cli-options)))

(defn usage-message [summary subcommand & [{:keys [show-preamble?]
                                            :or {show-preamble? true}}]]
  (let [preamble
        (when show-preamble?
          (->> [(str program-name " " program-version)
                ""
                (str program-name " "
                     "is a tool for working with search term log files")
                ""]
               (s/join \newline)))]
    (->> [preamble
          "Usage"
          (str "  java -jar " program-jar-path " [SUBCOMMAND] [OPTIONS]")
          ""
          "Options"
          summary
          ""
          "Subcommands"
          (->> (dissoc cli-options :help)
               (map (fn [[subcommand options]]
                      (str "  " (name subcommand) \newline
                           (s/join \newline
                                   (map #(str "    " (nth % 1)) options)))))
               (s/join "\n\n"))]
         (s/join \newline))))

(defn error-message [{:keys [raw-args errors] :as parsed-opts} subcommand]
  (str "The following errors occurred while parsing your command:"
       " "
       "`" program-name " " (s/join " " raw-args) "`"
       "\n\n"
       (s/join \newline errors)
       "\n\n"
       "Run `"
       program-name " --help"
       "` for more information"))

(defn valid-command? [{:keys [arguments summary options errors] :as parsed-opts}]
  (empty? errors))

(defn dispatch-command
  [{:keys [arguments summary options raw-args] :as parsed-opts} subcommand]
  (cond
    (or (nil? subcommand)
        (= :help subcommand)
        (:help options)
        (contains? (set arguments) "help")) {:stdout (usage-message summary
                                                                    subcommand)
                                             :return-code 0}
    (:version options) {:stdout version
                        :return-code 0}
    (and subcommand
         (valid-command? parsed-opts)) {:stdout
                                        ((get runners subcommand) options)
                                        :return-code 0}
    :else {:stdout
           (str "Invalid command: "
                "`"
                program-name (when raw-args (str " " (s/join " " raw-args)))
                "`"
                \newline
                (usage-message summary subcommand {:show-preamble? false}))
           :return-code 1}))

(defn -main
  "Entry-point to search-engine-indexer."
  [& args]
  (let [{:keys [options arguments] :as parsed-opts}
        (parse-opts args (:help cli-options))
        possible-subcommand (first args)
        subcommand-likely? (= possible-subcommand (first arguments))]
    (if-let [subcommand (keyword (if subcommand-likely?
                                   possible-subcommand
                                   "help"))]
      (if (contains? available-subcommands subcommand)
        (let [{:keys [arguments errors] :as parsed-opts}
              (assoc (parse-opts args (get cli-options subcommand))
                     :raw-args args)]
          (if errors
            (println (error-message parsed-opts subcommand))
            (if (< 1 (count arguments))
              (do
                (println "More than one subcommand given:" arguments)
                (println "Available subcommands:" available-subcommands))
              (let [{:keys [stdout return-code]}
                    (dispatch-command parsed-opts subcommand)]
                (println stdout)
                ;; (System/exit return-code)
                ))))
        (do
          (println "Subcommand" (name subcommand) "doesn't exist")
          (println "Available subcommands:" available-subcommands)))
      (let [{:keys [stdout return-code]}
            (dispatch-command parsed-opts nil)]
        (when-not (empty? stdout)
          (println stdout))
        ;; (System/exit return-code)
        ))))
