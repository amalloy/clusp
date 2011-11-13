(ns snusp.core
  (:use [useful.fn :only [fix !]]
        useful.debug))

(defn move
  "Advance pos one unit in the current direction."
  [world]
  (update-in world [:pos] (partial map + (:dir world))))

(defn ensure-tape
  "Make certain the tape is long enough that the head can read/write
  at its current location."
  [world]
  (update-in world [:tape]
             (fn [tape]
               (if (= (:head world) (count tape))
                 (conj tape 0),
                 tape))))

(defn tape-fn
  "Build a function for modifying the tape at the current read head."
  [f]
  (fn [world]
    (update-in world [:tape (:head world)] f)))

(defn head-fn
  "Build a function for moving the read head according to a function."
  [f]
  (fn [world]
    (ensure-tape (update-in world [:head] f))))

(defn mirror
  "Build a function for changing the direction of execution by pi/2."
  [m]
  (let [m (into m (for [[k v] m]
                    [v k]))]
    (fn [world]
      (update-in world [:dir] m))))

(defn curr-tape
  "Read a value from the tape's read-head."
  [world]
  (get (:tape world) (:head world)))

(def actions {\+ (tape-fn inc)
              \- (tape-fn dec)
              \> (head-fn inc)
              \< (head-fn dec)
              \\ (mirror {[1 0] [0 1], [0 -1] [-1 0]})
              \/ (mirror {[0 1] [-1 0], [1 0] [0 -1]})
              \! move
              \? (fn test [world]
                   (fix world
                        (comp zero? curr-tape) move))
              \, (fn read [{[x & more] :inputs :as world}]
                   (-> world
                       (assoc-in [:tape (:head world)] x)
                       (assoc :inputs more)))
              \. (fn write [world]
                   (update-in world [:outputs]
                              conj (curr-tape world)))
              \@ (fn call [world]
                   (update-in world [:call-stack] conj
                              (select-keys world [:dir :pos])))
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
  (let [nl? #{\newline}
        lines (remove #(some nl? %) (partition-by nl? program-text))
        start (or (first (for [[y line] (map-indexed list lines)
                               :let [x (.indexOf line "$")]
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
                      (move (instruction world)))))
         (drop-while (! :done)))))

(def snusp (comp :outputs first snusp-states))

(defn run [file inputs]
  (snusp (slurp (str "/Users/akm/src/clojure/snusp/resources/" file ".snusp"))
         inputs))
