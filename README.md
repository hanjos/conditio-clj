A simple condition system for Clojure, without too much machinery.

# What 

Exception systems divide responsibilities in two parts: _signalling_ the exception (like `throw`), and _handling_ it (like `try/catch`), unwinding the call stack until a handler is found. The problem is, by the time the error reaches the right handler, the context that signalled the exception is mostly gone. This limits the recovery options available.

A condition system, like the one in Common Lisp, provides a more general solution by splitting responsibilities in _three_ parts: _signalling_ the condition, _handling_ it, and _restarting_ execution. The call stack is unwound only if that was the handling strategy chosen; it doesn't have to be. This enables novel recovery strategies and protocols, and can be used for things other than error handling.

[Beyond Exception Handling: Conditions and Restarts](https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html), chapter 19 of Peter Seibel's [Practical Common Lisp](https://gigamonkeys.com/book/), informs much of the descriptions (as one can plainly see; I hope he doesn't mind :grin:), terminology and tests.

# Why?

I haven't used Clojure in _years_, so this idea seemed as good an excuse as any :smile: It's a good opportunity to remove some cobwebs and check out some stuff, such as `deps.edn`, transducers, maybe `spec` (I said it has been some time...).

Let's see how far I go...