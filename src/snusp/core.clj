(ns snusp.core
  (:use [useful.fn :only [to-fix !]]
        [useful.map :only [keyed]]
        useful.debug))

(defn partition-around [pred coll]
  (remove (comp pred first)
          (partition-by pred coll)))

(defmacro world-fn [[main :as args] body]
  `(fn [{:keys [~@args] :as world#}]
     (assoc world# ~(keyword main) ~body)))

(def move (world-fn [pos dir]
            (map + pos dir)))

(def ensure-tape (world-fn [tape head]
                   (if (= head (count tape))
                     (conj tape 0)
                     tape)))

(defn tape-fn
  "Build a function for modifying the tape at the current read head."
  [f]
  (fn [world]
    (update-in world [:tape (:head world)] f)))

(defn head-fn [f]
  (comp ensure-tape (world-fn [head] (f head))))

(defn mirror
  "Build a function for changing the direction of execution by pi/2."
  [m]
  (let [m (into m (for [[k v] m]
                    [v k]))]
    (world-fn [dir]
      (m dir))))

(defn curr-tape
  "Read a value from the tape's read-head."
  [{:keys [tape head]}]
  (get tape head))

(def actions {\+ (tape-fn inc)
              \- (tape-fn dec)
              \> (head-fn inc)
              \< (head-fn dec)
              \\ (mirror {[1 0] [0 1], [0 -1] [-1 0]})
              \/ (mirror {[0 1] [-1 0], [1 0] [0 -1]})
              \! move
              \? (to-fix (comp zero? curr-tape) move)
              \, (fn read [{[x & more] :inputs :as world}]
                   (-> world
                       (assoc-in [:tape (:head world)] x)
                       (assoc :inputs more)))
              \. (fn write [world]
                   (update-in world [:outputs]
                              conj (curr-tape world)))
              \@ (world-fn [call-stack dir pos]
                   (conj call-stack (keyed [dir pos])))
              \# (fn end [world]
                   (if-let [stack (not-empty (:call-stack world))]
                     (-> world
                         (merge (peek stack))
                         (update-in [:call-stack] pop)
                         (move))
                     (assoc world :done true)))})

(def initial-world
  {:dir [0 1]
   :pos [0 0]
   :tape [0]
   :head 0
   :inputs []
   :outputs []})

(defn snusp-states [program-text inputs]
  (let [lines (partition-around #{\newline} program-text)
        start (or (first (for [[y line] (map-indexed list lines)
                               :let [x (.indexOf line \$)]
                               :when (not (neg? x))]
                           [y x]))
                  [0 0])
        program (vec
                 (for [line lines]
                   (vec
                    (for [c line]
                      (get actions c identity)))))]
    (->> (assoc initial-world :inputs inputs, :pos start)
         (iterate (fn [world]
                    (let [instruction (get-in program (:pos world))]
                      (if-not instruction
                        (throw (Exception. (print-str world))))
                      (move (instruction world))))))))

(defn snusp [program inputs]
  (->> (snusp-states program inputs)
       (drop-while (! :done))
       (first)
       (:outputs)))

(defn debug [program inputs]
  (->> (snusp-states program inputs)
       (take-while (! :done))))

(defn run [file inputs]
  (snusp (slurp (str "/home/akm/src/clojure/snusp/resources/" file ".snusp"))
         inputs))
