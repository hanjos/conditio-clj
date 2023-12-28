(ns org.sbrubbles.conditio.vars
  "A variation on `org.sbrubbles.conditio`, which uses vars instead of keywords.
  There is no machinery for restarts; dynamic variables cover all intended uses.

  Example usage:
  ```clojure
  (require '[org.sbrubbles.conditio.vars :as v])

  (v/defcondition condition)
  (def ^:dynamic *restart* (partial v/abort \"No restart defined!\"))

  (v/handle [condition #(*restart* %)]
    (binding [*restart* inc]
      (assert (= (condition 1)
                 2))))
  ```")

(defn abort
  "Throws an exception, taking an optional argument."
  [msg & args]
  (throw (ex-info (str msg) {:args args})))

(def ^:private SKIP (gensym "SKIP-"))

(defn skip
  "Returns a value which, when returned by a handler, means that it opted not
  to handle the condition, and the handler chain should try the next one in
  line.

  Also works as a handler."
  [& _]
  SKIP)

(declare handler-not-found)

(def ^:dynamic *handlers*
  "A map with the available handlers, stored as handler chains. Use `handle`
  to install new handlers.

  A handler is a function which takes any number of arguments and returns the
  end result. A handler chain is a list of handlers, to be run one at a time
  until the first non-`(skip)` value is returned."
  {#'handler-not-found (list (partial abort #'handler-not-found))})

(defn- run-handler
  "Runs handler chains, returning the first non-(skip) value."
  [chain args]
  (transduce (comp (map #(apply % args))
                   (drop-while #(= % SKIP))
                   (take 1))
             (completing (fn [_ x] (reduced x)))
             nil
             chain))

(defn signal
  "Signals the condition `v`, searching for a handler and returning whatever it
  returns. `v` is expected to be a var created with `defcondition`.

  Signals `handler-not-found` if a handler couldn't be found."
  [v & args]
  (if-let [chain (get *handlers* v)]
    (run-handler chain args)
    (handler-not-found {:condition v :args args})))

(defn- ->metadata
  "For def*'s. Converts values into a metadata map (for (meta))."
  [obj]
  (cond (string? obj) {:doc obj}
        (map? obj) obj
        :else {:metadata obj}))

(defmacro defcondition
  "Creates a new condition; basically, a function which `signal`s itself when
  called. `metadata` is expected to be either a doc-string or a metadata map."
  ([v] `(defcondition ~v {}))
  ([v metadata]
   `(def ~(with-meta v (merge (->metadata metadata)
                              {:arglists `'([& ~'args])}))
      (partial signal (var ~v)))))

(defcondition handler-not-found
  "A condition which signals when a handler couldn't be found.")

(defn- var-ize
  "For binding-style macros. Converts pairs into a map."
  [var-vals]
  (loop [ret []
         vvs (seq var-vals)]
    (if vvs
      (recur (conj (conj ret `(var ~(first vvs))) (second vvs))
             (next (next vvs)))
      (seq ret))))

(defn- merge-bindings
  "For `handle` macros. Adds a new handler to a handler chain."
  [chain-map bindings]
  (reduce-kv (fn [acc k v]
               (assoc acc k (if (contains? acc k)
                              (conj (get acc k) v)
                              (list v))))
             chain-map
             bindings))

(defmacro handle
  "Installs the given bindings in `*handlers*`, executes `body`, and returns
  its result."
  [bindings & body]
  `(binding [*handlers* (~merge-bindings *handlers*
                                         (hash-map ~@(var-ize bindings)))]
     ~@body))

(defn handle-fn
  "Returns a function, which will install the given handlers (in a
  var-function map) and then run `f`.

  This may be used to define a helper function which runs on a different
  thread, but needs the given handlers in place."
  [binding-map f]
  (with-bindings*
    {#'*handlers* (merge-bindings *handlers* binding-map)}
    (fn [] (bound-fn* f))))

(defn bind-fn
  "Returns a function, which will install the bindings in `binding-map`
   and then call `f` with the given arguments."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))