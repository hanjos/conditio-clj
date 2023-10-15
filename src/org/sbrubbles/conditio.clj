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
  (and (map? v) (contains? v ::id)))

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

(def ^:dynamic *handlers*
  "The known handlers.

  A handler is a function which takes a condition and returns the value
  `signal` should return. Use `handle` to register new handlers."
  {::handler-not-found abort
   ::restart-not-found abort})

(def ^:dynamic *restarts*
  "The known restarts.

  A restart is a function which recovers from conditions, expected to be called
  from a handler. Use `with` to register new restarts."
  {})

(defn- bind-fn-with*
  "Like `bound-fn*`, but binding the given map along with the thread
   bindings."
  [f map]
  (let [bindings (merge (get-thread-bindings) map)]
    (fn [& args]
      (apply with-bindings* bindings f args))))

(defn signal
  "Signals a condition, searching for a handler and returning whatever it
  returns. `id` and `args` will be used to create the condition.

  Signals `:org.sbrubbles.conditio/handler-not-found` if a handler couldn't
  be found."
  [id & {:as args}]
  (let [c (condition id args)]
    (if-let [handler (*handlers* (::id c))]
      (handler c)
      (signal ::handler-not-found :condition c))))

(defn with-fn
  "Returns a function, which will install the given restarts and then run `f`.
  This may be used to define a helper function which runs on a different
  thread, but needs the given restarts in place."
  [f restart-map]
  (bind-fn-with* f {#'*restarts* (merge *restarts* restart-map)}))

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
  `(binding [*handlers* (merge *handlers* (hash-map ~@bindings))]
     ~@body))
