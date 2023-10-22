(ns org.sbrubbles.conditio.vars
  "A variation on org.sbrubbles.conditio, using vars instead of keywords.
  That led to something quite different...

  Example usage:
  ```clojure
  (require '[org.sbrubbles.conditio.vars :as v])

  (v/defcondition *cc*)
  (v/defrestart *rr*)

  (binding [*cc* (fn [& args] (v/restart '*rr* (first args)))]
    ; ...
    (binding [*rr* inc]
      (assert (= (*cc* 1) 2))))
  ```
  ")

(defn- ->meta-map [maybe-map]
  (cond (string? maybe-map) {:doc maybe-map}
        (map? maybe-map) maybe-map
        :else {:args maybe-map}))

(defmacro defcondition
  "Creates a new var holding a signalling function. When called, it will
  throw an `ExceptionInfo`. The idea is for this var to be re-bound (via
  `binding`) to a handler."
  ([name] `(defcondition ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     ;; normal ^:dynamic syntax doesn't work inside macros
     `(def ~(with-meta name meta)
        (fn [& args#]
          (throw (ex-info ~(pr-str name) {:args args#})))))))

(defcondition *restart-not-found*
              "Signalled when a given restart isn't found.")

(defmacro defrestart
  "Creates a new var holding a restart function, which when called will signal
  `*restart-not-found*`. The idea is for this var to become a target for
  `restart` and `with`."
  ([name] `(defrestart ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (apply *restart-not-found* (conj args# ~(pr-str name))))))))

(defn restart
  "Resolves `sym` to a var expected to hold a function and runs it, with
  `args`. `sym` should be namespaced."
  [sym & args]
  (if-let [v (resolve sym)]
    (apply (var-get v) args)
    (*restart-not-found* sym args)))

(defn with-fn
  "Returns a function which, when run, will see the bindings in `binding-map`."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))
