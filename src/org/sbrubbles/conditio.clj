(ns org.sbrubbles.conditio)

(defn abort
  "Returns a restart which aborts by throwing an exception."
  ([] (abort "Abort"))
  ([msg] (fn [& args]
           (let [first-arg (first args)
                 suffix (or (and first-arg
                                 (or (and (> (count args) 1)
                                          (str " (" first-arg ", ...)"))
                                     (str " (" first-arg ")")))
                            "")]
             (throw (ex-info (str msg suffix) {:args args}))))))

(def ^:dynamic *handlers*
  "The known handlers. Use handle to register new handlers."
  {::handler-not-found (abort "Handler not found")
   ::restart-not-found (abort "Restart not found")})

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
  "Signals a new condition, returning whatever the handler for that condition
  returns.

  This function itself signals
  :org.sbrubbles.conditio/handler-not-found if a handler couldn't be found."
  [condition & args]
  (if-let [handler (*handlers* condition)]
    (apply handler args)
    (signal ::handler-not-found condition)))

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
  "Returns a function, which searches for a restart mapped to the given
  option, and then runs it with args.

  Signals :org.sbrubbles.conditio/restart-not-found if no restart mapped to
  option could be found."
  [option & args]
  (fn [& _]
    (if-let [restart (*restarts* option)]
      (apply restart args)
      (signal ::restart-not-found option))))

(defmacro handle
  "Takes a map of keyword/handler pairs. Then this macro:

  (1) installs the given handlers as a thread-local binding;
  (2) executes the given body;
  (3) pops the given handlers after body was evaluated; and
  (4) returns the value of body."
  [bindings & body]
  `(binding [*handlers* (merge *handlers* (hash-map ~@bindings))]
     ~@body))
