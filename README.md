A simple condition system for Clojure, without too much machinery.

[![CI](https://github.com/hanjos/conditio-clj/actions/workflows/ci.yml/badge.svg)](https://github.com/hanjos/conditio-clj/actions/workflows/ci.yml) [![Docs](https://img.shields.io/static/v1?label=Docs&message=0.1.0&color=informational&logo=read-the-docs)][vLatest] [![Git package](https://img.shields.io/static/v1?label=Git&message=0.1.0&color=red&logo=git)][GitPackage] [![Maven package](https://img.shields.io/static/v1?label=Maven&message=0.1.0&color=orange&logo=apache-maven)][MavenPackage]

# Latest

### Git
```
org.sbrubbles/conditio-clj {:git/url "https://github.com/hanjos/conditio-clj" 
                            :git/tag "0.1.0"  :git/sha "1be4a0a"}
```

### Maven 
Configure your [`settings.xml`](https://stackoverflow.com/a/58453517):
```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_LOGIN</username>
        <password>YOUR_AUTH_TOKEN</password>
    </server>
</servers>
```

Add this repo to your `deps.edn`:
```
:mvn/repos {"github" {:url "https://maven.pkg.github.com/hanjos/conditio-clj"}}
```

And then:
```
org.sbrubbles/conditio-clj {:mvn/version "0.1.0"}
```                                                                       

# What 

Exception systems divide responsibilities in two parts: _signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`), unwinding the call stack until a handler is found. The problem is, by the time the error reaches the right handler, the context that signalled the exception is mostly gone. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution by splitting responsibilities in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols, and can be used for things other than error handling.

[Beyond Exception Handling: Conditions and Restarts](https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html), chapter 19 of Peter Seibel's [Practical Common Lisp](https://gigamonkeys.com/book/), informs much of the descriptions (as one can plainly see; I hope he doesn't mind :grin:), terminology and tests.

# Why?

I haven't used Clojure in _years_, so this seemed as good an excuse as any :smile: 

It's an opportunity to remove some cobwebs and check out some "new" stuff, such as `deps.edn`, transducers, maybe `spec` (I said it has been some time...). Let's see how far I go...

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
    (c/with [::retry-with parse-log-entry]
      ; signals :user/malformed-log-entry 
      (c/signal ::malformed-log-entry :line line))))

(defn parse-log-file []
  ; creates a function which calls parse-log-entry with :user/skip-entry 
  ; as an available restart  
  (comp (map (c/with-fn parse-log-entry
                        {::skip-entry (fn [] ::skip-entry)}))
        (filter #(not (= % ::skip-entry)))))

(defn analyze-logs [& args]
  ; handles :user/malformed-log-entry conditions, restarting with 
  ; :user/skip-entry
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

`deps.edn`, `tools.build` and [codox](https://github.com/weavejester/codox) for docs. 

# Caveats and stuff to mull over
* Despite what the name might suggest, I didn't try to maintain parity with [conditio-java](https://github.com/hanjos/conditio-java). Particularly, there's no skipping handlers here. Sounds interesting to have, though...
* I could've used vars instead of keywords. I'm not sure which one works better; so [some experiments](DESIGN.md) are in order...


[vLatest]: https://sbrubbles.org/conditio-clj/docs/0.1.0/index.html
[MavenPackage]: https://github.com/hanjos/conditio-clj/packages/1968125
[GitPackage]: https://github.com/hanjos/conditio-clj/tree/1be4a0a7da7e2026bda32683ef10bbf5bf40bcbe
