(ns org.sbrubbles.conditio)

(def ^:dynamic *handlers*
  {::handler-not-found #(throw (ex-info (str "Handler not found (" % ")")
                                        {:condition %}))
   ::restart-not-found #(throw (ex-info (str "Restart not found (" % ")")
                                        {:option %}))})

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
