(ns org.sbrubbles.conditio.vars
  "A variation on `org.sbrubbles.conditio`, using vars instead of keywords.

  Example usage:
  ```clojure
  (require '[org.sbrubbles.conditio.vars :as v])

  (v/defcondition *cc*)
  (v/defrestart *rr*)

  (binding [*cc* (fn [& args] (*rr* (first args)))]
    (binding [*rr* inc]
      (assert (= (*cc* 1) 2))))
  ```
  "
  (:import (java.lang.reflect InaccessibleObjectException)))

(defn- ->meta-map [maybe-map]
  (cond (string? maybe-map) {:doc maybe-map}
        (map? maybe-map) maybe-map
        :else {:args maybe-map}))

(defmacro defcondition
  "Creates a new var holding a signalling function, which, when called, will
  throw an `ExceptionInfo`. The idea is for this var to be re-bound (via
  `binding`) to a function which will handle the condition."
  ([name] `(defcondition ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (throw (ex-info ~(pr-str name) {:args args#})))))))

(defcondition *restart-not-found*
              "Signalled when a given restart isn't found.")

(defmacro defrestart
  "Creates a new var holding a restart function, which, when called, will
  signal `*restart-not-found*`. The idea is for this var to be re-bound (via
  `binding`) to a function, which will generate the end result."
  ([name] `(defrestart ~name nil))
  ([name meta-map]
   (let [meta (merge (->meta-map meta-map)
                     {:dynamic true})]
     `(def ~(with-meta name meta)
        (fn [& args#]
          (apply *restart-not-found* (conj args# ~(pr-str name))))))))

(def ^:private SKIP (gensym))

(defn skip
  "Returns a value which, when returned by a handler, means that it opted not
  to handle the condition, and another handler should be tried.

  This function works as a handler by itself, and returns the same value."
  [& _]
  SKIP)

(defn- field-get
  "Returns the value in the field `<object>.<name>`, or `not-found` if the
  field wasn't found or accessible."
  ([object name]
   (field-get object name nil))
  ([object name not-found]
   (try
     (let [field (-> (class object)
                     (.getDeclaredField name))]
       (.setAccessible field true)
       (.get field object))
     (catch NullPointerException _
       not-found)
     (catch NoSuchFieldException _
       not-found)
     (catch SecurityException _
       not-found)
     (catch InaccessibleObjectException _
       not-found))))

(defn signal
  "Searches for a handler for `v`, and invokes it with the given args.

  `v` is expected to be a Var pointing to a condition."
  [^clojure.lang.Var v & args]
  (loop [f (clojure.lang.Var/getThreadBindingFrame)]
    (if (nil? f)
      ((.getRawRoot v) args)
      (let [handler (-> f
                        (field-get "bindings")
                        (get v)
                        (field-get "val" skip))
            value (handler args)]
        (if (not= value (skip))
          value
          (recur (field-get f "prev")))))))

(defn bind-fn
  "Returns a function, which will install the bindings in `binding-map`
  and then call `f` with the given arguments."
  [binding-map f]
  (with-bindings* binding-map (fn [] (bound-fn* f))))