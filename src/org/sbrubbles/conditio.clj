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
  "Creates a new condition, or returns it unchanged if it's the sole argument."
  ([c] (condition c nil))
  ([id & {:as args}]
   (assert (not (nil? id)))
   (if (nil? args)
     (if (condition? id) id {::id id})
     (assoc args ::id id))))

(defn abort
  "Throws an exception, taking an optional argument. Also works as a handler."
  ([] (abort nil))
  ([c]
   (let [msg (cond (nil? c) "Abort"
                   (condition? c) (str "Abort: " (::id c))
                   :else (str "Abort on " c))]
     (throw (ex-info msg {:args c})))))

(defn skip
  "Returns a value which, when returned by a handler, means that it opted not
  to handle the condition, and the handler chain should try another handler.

  The 1-arity version is there to work as a handler, and returns the same
  value."
  ([] ::skip)
  ([_] ::skip))

(def ^:dynamic *handlers*
  "The available handlers.

  A handler is a function which takes a condition and returns the value
  `signal` should return. Use `handle` to register new handlers."
  {::handler-not-found (list abort)
   ::restart-not-found (list abort)})

(def ^:dynamic *restarts*
  "The available restarts.

  A restart is a function which recovers from conditions, expected to be called
  from a handler. Use `with` to register new restarts."
  {})

(defn- bind-fn
  "Returns a function, which will install the bindings in `binding-map`
   and then call `f` with the given arguments."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))

(defn seek
  "Takes a transducer (`xf`) and a coll (`coll`), returning the first element
  in coll which makes it past `xf`, suitably transformed and filtered."
  [xf coll]
  (transduce xf
             (completing (fn [_ x] (reduced x)))
             nil
             coll))

(defn ->handler
  "Returns a handler equivalent to the given handler chain, or `nil` if the
  given chain is nil.

  A handler chain is a sequence of handlers, to be run one at a time until
  the first non-`(skip)` value is returned."
  [chain]
  (when chain
    (fn [c]
      (seek (comp (map #(% c))
                  (drop-while #(= % ::skip))
                  (take 1))
            chain))))

(defn merge-handlers
  "Expects a map of handler chains (like `*handlers*`) and a map of
  keyword/handler pairs, and returns a map of handler chains, with the
  bindings conj-ed in."
  [chain-map bindings]
  (reduce-kv (fn [acc k v]
               (println acc k v)
               (if (contains? acc k)
                 (assoc acc k (conj (get acc k) v))
                 (assoc acc k (list v abort))))
             chain-map
             bindings))

(defn signal
  "Signals a condition, searching for a handler and returning whatever it
  returns. `id` and `args` will be used to create the condition.

  Signals `:org.sbrubbles.conditio/handler-not-found` if a handler couldn't
  be found."
  [id & {:as args}]
  (let [c (condition id args)]
    (if-let [handler (->handler (get *handlers* (::id c)))]
      (handler c)
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

  Signals `:org.sbrubbles.conditio/restart-not-found` if no restart mapped to
  option could be found."
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
  `(binding [*handlers* (merge-handlers *handlers* (hash-map ~@bindings))]
     ~@body))
