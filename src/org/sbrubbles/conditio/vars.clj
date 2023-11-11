(ns org.sbrubbles.conditio.vars
  "A variation on `org.sbrubbles.conditio`, using vars instead of keywords.

  Example usage:
  ```clojure
  (require '[org.sbrubbles.conditio.vars :as v])

  (v/defcondition *cc*)
  (v/defrestart *rr*)

  (binding [*cc* (fn [& args] (*rr* (first args)))]
    (binding [*rr* inc]
      (assert (= (*cc* 1) 2))))
  ```
  ")

(defn- ->meta-map [maybe-map]
  (cond (string? maybe-map) {:doc maybe-map}
        (map? maybe-map) maybe-map
        :else {:args maybe-map}))

(defmacro defcondition
  "Creates a new var holding a signalling function, which, when called, will
  throw an `ExceptionInfo`. The idea is for this var to be re-bound (via
  `binding`) to a function which will handle the condition."
  ([name] `(defcondition ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (throw (ex-info ~(pr-str name) {:args args#})))))))

(defcondition *restart-not-found*
              "Signalled when a given restart isn't found.")

(defmacro defrestart
  "Creates a new var holding a restart function, which, when called, will
  signal `*restart-not-found*`. The idea is for this var to be re-bound (via
  `binding`) to a function, which will generate the end result."
  ([name] `(defrestart ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (apply *restart-not-found* (conj args# ~(pr-str name))))))))

(defn bind-fn
  "Returns a function, which will install the bindings in `binding-map`
  and then call `f` with the given arguments."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))