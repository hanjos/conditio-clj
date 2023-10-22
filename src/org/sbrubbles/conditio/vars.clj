(ns org.sbrubbles.conditio.vars)

(defn- ->meta-map [maybe-map]
  (cond (string? maybe-map) {:doc maybe-map}
        (map? maybe-map) maybe-map
        :else {:args maybe-map}))

(defmacro defcondition
  ([name] `(defcondition ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     ;; normal ^:dynamic syntax doesn't work inside macros
     `(def ~(with-meta name meta)
        (fn [& args#]
          (throw (ex-info ~(pr-str name) {:args args#})))))))

(defcondition *restart-not-found*)

(defmacro defrestart
  ([name] `(defrestart ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (apply *restart-not-found* (conj args# ~(pr-str name))))))))

(defn restart [sym & args]
  (if-let [v (resolve sym)]
    (apply (var-get v) args)
    (*restart-not-found* sym args)))

(defmacro with
  [bindings f]
  (let [var-ize (fn [var-vals]
                  (loop [ret [] vvs (seq var-vals)]
                    (if vvs
                      (recur (conj (conj ret `(var ~(first vvs))) (second vvs))
                             (next (next vvs)))
                      (seq ret))))]
    `(with-fn (hash-map ~@(var-ize bindings)) ~f)))

(defn with-fn [map f]
  (with-bindings* map (fn [] (bound-fn* f))))
