(ns correct-tags.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:gen-class))

(defn parse-tag [tag]
  (let [tags (-> tag 
                 (str/replace #"\(" "")
                 (str/replace #"\)" "")
                 (str/split #"_"))]
    {:type (first tags)
     :iob (second tags)}))

(defn parse-tok [tok]
  (let [tok-elements (re-matches #"(.+)(\([^\)]+\))" tok)]
    (if (nil? tok-elements)
      {:w tok}
      {:w (second tok-elements)
       :tag (parse-tag (nth tok-elements 2))})))

(defn parse-line [line]
  (->> (str/split line #"\|")
       (remove empty?)
       (map parse-tok)))

(defn make-chunks* [chunks tok]
  (cond
    (= (->> tok :tag :iob) "start")
    (cons {:toks (list tok) :state :init}
          chunks)

    (= (->> tok :tag :iob) "end")
    (cons (-> (first chunks)
              (update :toks #(cons tok %))
              (assoc :state :complete))
          (rest chunks))

    (= (->> chunks first :state) :init)
    (cons (-> (first chunks)
              (update :toks #(cons tok %)))
          (rest chunks))
    
    :else (cons tok chunks)))

(defn adjust-toks [toks]
  (->> toks
       reverse
       (into [])))

(defn adjust-toks-in-chunk [chunk-or-w]
  (if (:toks chunk-or-w)
    (update chunk-or-w :toks adjust-toks)
    chunk-or-w))

(defn make-chunks [toks]
  (->> toks
       (reduce make-chunks* '())
       (map adjust-toks-in-chunk)
       reverse))

(defn load-annot [path]
  (with-open [r (io/reader path)]
    (->> r
         line-seq
         (map parse-line)
         (map make-chunks)
         (into []))))

(defn invalid-types? [toks]
  (let [types (map (comp :type :tag) toks)
        not-nil-unique-types-cnt (->> types
                                      (remove nil?)
                                      distinct
                                      count)]
    (or (nil? (first types))
        (nil? (last types))
        (> not-nil-unique-types-cnt 1))))

(defn invalid-state? [chunk]
  (not= (:state chunk) :complete))

(defn invalid-chunk? [chunk]   
  (or (invalid-state? chunk)
      (invalid-types? (:toks chunk))))

(defn invalid-line? [line]
  (some invalid-chunk? (filter :toks line)))

(defn verify-annot [lines]
  (not (some invalid-line? lines)))

(defn normalize-tok-in-chunk [type tok]
  (if (->> tok :tag :iob)
    tok
    (assoc tok :tag {:type type :iob "cont"})))

(defn normalize-toks-in-chunk [type toks]
  (map #(normalize-tok-in-chunk type %) toks))

(defn normalize-chunk [chunk]
  (let [type (-> chunk :toks first :tag :type)]
    (update chunk :toks #(normalize-toks-in-chunk type %))))

(defn normalize-chunk-or-tok [chunk-or-tok]
  (if (:toks chunk-or-tok)
    (normalize-chunk chunk-or-tok)
    chunk-or-tok))

(defn normalize-line [line]
  (map normalize-chunk-or-tok line))

(defn break-chunks* [line chunk-or-tok]
  (if (:toks chunk-or-tok)
    (into [] (concat line (:toks chunk-or-tok)))
    (conj line chunk-or-tok)))

(defn break-chunks [line]
  (reduce break-chunks* [] line))

(defn dump-tok [tok]
  (cond
    (-> tok :tag :iob)
    (str (:w tok) "(" (-> tok :tag :type) "_" (-> tok :tag :iob) ")")
    
    (:tag tok)
    (str (:w tok) "(" (-> tok :tag :type) ")")

    :else
    (str (:w tok))))

(defn dump-line [line]
  (str "|"
       (str/join "|" (->> line
                          break-chunks
                          (map dump-tok)))
       "|"))

(defn dump-annot [annot]
  (str/join "\n" (map dump-line annot)))

(defn normalize* [annot normalized-annot-path]
  (spit normalized-annot-path
        (->> annot
             (map normalize-line)
             dump-annot)))

(defn normalize [annot-path normalized-annot-path]
  (let [annot (load-annot annot-path)]
    (if (not (verify-annot annot))
      (println (annot-path "is invalid"))
      (do
        (normalize* annot normalized-annot-path)
        (println (str annot-path " was normalized."))))))

(defn usage []
  (println "Usage: lein run <annot file path> <normalized annot file path>"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (= 2 (count args))
    (apply normalize (take 2 args))
    (usage)))
