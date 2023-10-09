(ns org.sbrubbles.conditio)

(defn abort
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
  {::handler-not-found (abort "Handler not found")
   ::restart-not-found (abort "Restart not found")})

(def ^:dynamic *restarts*
  {})

(defn- bind-fn-with*
  [f map]
  (let [bindings (merge (get-thread-bindings) map)]
    (fn [& args]
      (apply with-bindings* bindings f args))))

(defn signal
  [condition & args]
  (if-let [handler (*handlers* condition)]
    (apply handler args)
    (signal ::handler-not-found condition)))

(defn with-restarts-fn
  [f restart-map]
  (bind-fn-with* f {#'*restarts* (merge *restarts* restart-map)}))

(defmacro with-restarts
  [bindings & body]
  `(binding [*restarts* (merge *restarts* (hash-map ~@bindings))]
     ~@body))

(defn use-restart
  [option & args]
  (fn [& _]
    (if-let [restart (*restarts* option)]
      (apply restart args)
      (signal ::restart-not-found option))))

(defn with-handlers-fn
  [f handler-map]
  (bind-fn-with* f {#'*handlers* (merge *handlers* handler-map)}))

(defmacro handle
  [bindings & body]
  `(binding [*handlers* (merge *handlers* (hash-map ~@bindings))]
     ~@body))
