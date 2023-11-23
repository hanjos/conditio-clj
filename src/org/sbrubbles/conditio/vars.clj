(ns org.sbrubbles.conditio.vars)

(defn abort
  ([msg]
   (partial abort msg))
  ([msg & args]
   (throw (ex-info (str msg) {:args args}))))

(def ^:private SKIP (gensym "SKIP-"))

(defn skip
  [& _]
  SKIP)

(declare handler-not-found)
(declare restart-not-found)

(def ^:dynamic *handlers* {#'handler-not-found (list (abort #'handler-not-found))
                           #'restart-not-found (list (abort #'restart-not-found))})
(def ^:dynamic *restarts* {})

(defn- run-handler
  [chain args]
  (transduce (comp (map #(apply % args))
                   (drop-while #(= % SKIP))
                   (take 1))
             (completing (fn [_ x] (reduced x)))
             nil
             chain))

(defn signal [v & args]
  (if-let [chain (get *handlers* v)]
    (run-handler chain args)
    (handler-not-found {:condition v :args args})))

(defn- ->metadata
  [obj]
  (cond (string? obj) {:docs obj}
        (map? obj) obj
        :else {:data obj}))

(defmacro defcondition
  ([v] `(defcondition ~v {}))
  ([v metadata]
   `(def ~(with-meta v (->metadata metadata))
      (fn [& args#]
        (signal ~(resolve v) args#)))))

(defcondition handler-not-found)
(defcondition restart-not-found)

(defmacro handle
  [bindings & body]
  (let [var-ize (fn [var-vals]
                  (loop [ret []
                         vvs (seq var-vals)]
                    (if vvs
                      (recur (conj (conj ret `(var ~(first vvs))) (second vvs))
                             (next (next vvs)))
                      (seq ret))))]
    `(let [merge-bindings# (fn [chain-map# bindings#]
                             (reduce-kv (fn [acc# k# v#]
                                          (assoc acc# k# (if (contains? acc# k#)
                                                           (conj (get acc# k#) v#)
                                                           (list v#))))
                                        chain-map#
                                        bindings#))]
       (binding [*handlers* (merge-bindings# *handlers*
                                             (hash-map ~@(var-ize bindings)))]
         ~@body))))

(defn restart
  [v & args]
  (if-let [r (get *restarts* v)]
    (apply r args)
    (restart-not-found {:condition v :args args})))

(defmacro defrestart
  ([v] `(defrestart ~v {}))
  ([v metadata]
   `(def ~(with-meta v (->metadata metadata))
      (fn [& args#]
        (apply restart ~(resolve v) args#)))))

(defmacro with
  [bindings & body]
  (let [var-ize (fn [var-vals]
                  (loop [ret []
                         vvs (seq var-vals)]
                    (if vvs
                      (recur (conj (conj ret `(var ~(first vvs))) (second vvs))
                             (next (next vvs)))
                      (seq ret))))]
    `(binding [*restarts* (merge *restarts* (hash-map ~@(var-ize bindings)))]
       ~@body)))

(defn with-fn
  [binding-map f]
  (with-bindings*
    {#'*restarts* (merge *restarts* binding-map)}
    (fn [] (bound-fn* f))))