A simple condition system for Clojure, without too much machinery.

[![CI](https://github.com/hanjos/conditio-clj/actions/workflows/ci.yml/badge.svg)](https://github.com/hanjos/conditio-clj/actions/workflows/ci.yml)

# What 

Exception systems divide responsibilities in two parts: _signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`), unwinding the call stack until a handler is found. The problem is, by the time the error reaches the right handler, the context that signalled the exception is mostly gone. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution by splitting responsibilities in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols, and can be used for things other than error handling.

[Beyond Exception Handling: Conditions and Restarts](https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html), chapter 19 of Peter Seibel's [Practical Common Lisp](https://gigamonkeys.com/book/), informs much of the descriptions (as one can plainly see; I hope he doesn't mind :grin:), terminology and tests.

# Why?

I haven't used Clojure in _years_, so this idea seemed as good an excuse as any :smile: 

It's a good opportunity to remove some cobwebs and check out some "new" stuff, such as `deps.edn`, transducers, maybe `spec` (I said it has been some time...). Let's see how far I go...

# How?

Well, with `binding` and dynamic variables, most of the machinery is already there, so life is a lot easier :smile:

The end result should look something like this:

```clojure
(ns user
  (:require [org.sbrubbles.conditio :as c]))

; This example draws from Practical Common Lisp, but with some shortcuts 
; to simplify matters 

(defn parse-log-entry [line]
  (if (not (= line :fail)) ; :fail represents a malformed log entry
    line
    ; adds :user/retry-with as an available restart
    ; Common Lisp has a :use-value restart, but since here the handler
    ; returns the result to use, it didn't seem necessary
    (c/with [::retry-with parse-log-entry]
      ; signals :user/malformed-log-entry 
      (c/signal ::malformed-log-entry :line line))))

(defn parse-log-file []
  ; creates a function which calls parse-log-entry with :user/skip-entry 
  ; available as a restart  
  (comp (map (c/with-fn parse-log-entry
                        {::skip-entry (fn [] ::skip-entry)}))
        (filter #(not (= % ::skip-entry)))))

(defn analyze-logs [& args]
  ; handles :user/malformed-log-entry conditions, selecting 
  ; :user/skip-entry as the restart to use
  (c/handle [::malformed-log-entry (fn [_] (c/restart ::skip-entry))]
    (into []
          (comp cat
                (parse-log-file))
          args)))

; every vector is a 'file'
(analyze-logs ["a" "b"]
              ["c" :fail :fail]
              [:fail "d" :fail "e"])
;; => ["a" "b" "c" "d" "e"]
```

# Using

`deps.edn`, `tools.build` and [codox](https://github.com/weavejester/codox) for docs. Still gotta figure out how to generate and publish versions, but that shouldn't be too hard...

# Caveats and stuff to mull over
* Despite what the name might suggest, I didn't try to maintain parity with [conditio-java](https://github.com/hanjos/conditio-java). Particularly, there's no skipping handlers here. Sounds interesting to have, but might complicate the implementation.
* I could've used vars instead of keywords. I'm not sure which one works better; so some experiments are in order...