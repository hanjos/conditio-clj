(ns org.sbrubbles.conditio)

(defn condition?
  "Checks if the given value is a condition."
  [v]
  (and (map? v) (contains? v ::id)))

(defn condition
  "Creates a new condition. A condition is a map with
  an :org.sbrubbles.conditio/id key (with a non-nil value), which is used to
  identify a handler for it.

  This function, if given only a prebuilt condition, will return it unchanged.
  Otherwise, it will create a new condition with the given arguments."
  [id & {:as map}]
  (assert (not (nil? id)))
  (if (nil? map)
    (if (condition? id) id {::id id})
    (assoc map ::id id)))

(defn abort
  "Takes an optional condition, and aborts by throwing an exception."
  ([] (abort nil))
  ([c]
   (let [msg (cond (nil? c) "Abort"
                   (condition? c) (str "Abort: " (::id c))
                   :else (str "Abort on " c))]
     (throw (ex-info msg {:args c})))))

(def ^:dynamic *handlers*
  "The known handlers. Use handle to register new handlers."
  {::handler-not-found abort
   ::restart-not-found abort})

(def ^:dynamic *restarts*
  "The known restarts. Use with to register new restarts."
  {})

(defn- bind-fn-with*
  "Like bound-fn*, but binding the given map as well along with the thread
   bindings."
  [f map]
  (let [bindings (merge (get-thread-bindings) map)]
    (fn [& args]
      (apply with-bindings* bindings f args))))

(defn signal
  "Signals a condition, returning whatever the handler for that condition
  returns. The arguments will be used to create the condition to signal
  (as in org.sbrubbles.conditio/condition).

  This function itself signals
  :org.sbrubbles.conditio/handler-not-found if a handler couldn't be found."
  [id & {:as args}]
  (let [c (condition id args)]
    (if-let [handler (*handlers* (::id c))]
      (handler c)
      (signal ::handler-not-found :condition c))))

(defn with-fn
  "Returns a function, which will install the given restarts and then run
  with any given arguments. This may be used to define a helper function
  which runs on a different thread, but needs the given restarts in place."
  [f restart-map]
  (bind-fn-with* f {#'*restarts* (merge *restarts* restart-map)}))

(defmacro with
  "Takes a map of keyword/restart pairs. Then this macro:

  (1) installs the given restarts as a thread-local binding;
  (2) executes the given body;
  (3) pops the given restarts after body was evaluated; and
  (4) returns the value of body."
  [bindings & body]
  `(binding [*restarts* (merge *restarts* (hash-map ~@bindings))]
     ~@body))

(defn restart
  "Searches for a restart mapped to the given option, and then runs it with
  args.

  Signals :org.sbrubbles.conditio/restart-not-found if no restart mapped to
  option could be found."
  [option & args]
  (if-let [restart (*restarts* option)]
    (apply restart args)
    (signal ::restart-not-found :option option)))

(defmacro handle
  "Takes a map of keyword/handler pairs. A handler is a function which takes
  a condition and returns the value signal should return.

  This macro:

  (1) installs the given handlers as a thread-local binding;
  (2) executes the given body;
  (3) pops the given handlers after body was evaluated; and
  (4) returns the value of body."
  [bindings & body]
  `(binding [*handlers* (merge *handlers* (hash-map ~@bindings))]
     ~@body))
