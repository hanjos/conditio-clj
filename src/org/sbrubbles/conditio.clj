(ns org.sbrubbles.conditio
  "A simple condition library.

  Example usage:
  ```clojure
  (require '[org.sbrubbles.conditio :as c])

  ;; ...

  (c/handle [:condition #(c/restart :restart (:n %))]
    (c/with [:restart inc]
      (assert (= (c/signal :condition :n 1)
                 2))))
  ```")

(defn condition?
  "Checks if the given value is a condition: a map with
  `:org.sbrubbles.conditio/id` mapped to a non-`nil` value."
  [v]
  (and (map? v) (some? (::id v))))

(defn condition
  "Creates a new condition, or returns it unchanged if it's the sole argument.
  `id` cannot be nil."
  ([c] (condition c nil))
  ([id & {:as args}]
   (assert (not (nil? id)))
   (cond (not (nil? args)) (assoc args ::id id)
         (condition? id) id
         :else {::id id})))

(defn abort
  "Throws an exception, taking an optional argument. The 1-arity version also
  works as a handler."
  ([] (abort nil))
  ([c]
   (let [msg (cond (nil? c) "Abort"
                   (condition? c) (str "Abort: " (::id c))
                   :else (str "Abort on " c))]
     (throw (ex-info msg {:args c})))))

(def ^:private SKIP (gensym))

(defn skip
  "Returns a value which, when returned by a handler, means that it opted not
  to handle the condition, and the handler chain should try the next one in
  line.

  The 1-arity version works as a handler, and returns the same value."
  ([] SKIP)
  ([_] SKIP))

(def ^:dynamic *handlers*
  "A map with the available handlers, stored as handler chains. Use `handle`
  to install new handlers.

  A handler is a function which takes a condition and returns the value
  `signal` should return. A handler chain is a list of handlers, to be
  run one at a time until the first non-`(skip)` value is returned."
  {::handler-not-found (list abort)
   ::restart-not-found (list abort)})

(def ^:dynamic *restarts*
  "A map with the available restarts. Use `with` to install new restarts, and
  `restart` to run them.

  A restart is a function which recovers from conditions, expected to be called
  from a handler."
  {})

(defn- bind-fn
  "Returns a function, which will install the bindings in `binding-map`
   and then call `f` with the given arguments."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))

(defn- chain-handle
  "Attempts to handle the condition (`c`) with the given handler chain
  (`chain`)."
  [chain c]
  (transduce (comp (map #(% c))
                   (drop-while #(= % SKIP))
                   (take 1))
             (completing (fn [_ x] (reduced x)))
             nil
             chain))

(defn signal
  "Signals a condition, searching for a handler and returning whatever it
  returns. `id` and `args` will be used to create the condition, as in
  `condition`.

  Signals `:org.sbrubbles.conditio/handler-not-found` if a handler couldn't
  be found."
  [id & {:as args}]
  (let [c (condition id args)]
    (if-let [chain (get *handlers* (::id c))]
      (chain-handle chain c)
      (signal ::handler-not-found :condition c))))

(defn with-fn
  "Returns a function, which will install the given restarts and then run `f`.
  This may be used to define a helper function which runs on a different
  thread, but needs the given restarts in place."
  [restart-map f]
  (bind-fn {#'*restarts* (merge *restarts* restart-map)} f))

(defmacro with
  "Takes a map of keyword/restart pairs, and then:

  1. installs the given restarts as a thread-local binding;
  2. executes the given body;
  3. pops the given restarts after body was evaluated; and
  4. returns the value of body."
  [bindings & body]
  `(binding [*restarts* (merge *restarts* (hash-map ~@bindings))]
     ~@body))

(defn restart
  "Searches for a restart mapped to `option`, and then runs it with `args`.

  Signals `:org.sbrubbles.conditio/restart-not-found` if no restart could be
  found."
  [option & args]
  (if-let [restart (*restarts* option)]
    (apply restart args)
    (signal ::restart-not-found :option option)))

(defmacro handle
  "Takes a map of keyword/handler pairs. This macro:

  1. installs the given handlers as a thread-local binding;
  2. executes the given body;
  3. pops the given handlers after body was evaluated; and
  4. returns the value of body."
  [bindings & body]
  `(let [merge-handlers# (fn [chain-map# bindings#]
                           (reduce-kv (fn [acc# k# v#]
                                        (assoc acc# k# (if (contains? acc# k#)
                                                         (conj (get acc# k#) v#)
                                                         (list v# abort))))
                                      chain-map#
                                      bindings#))]
     (binding [*handlers* (merge-handlers# *handlers* (hash-map ~@bindings))]
       ~@body)))