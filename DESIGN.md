# Thoughts and speculations on design

## 0.2.0

### vars vs keywords

`org.sbrubbles.conditio` uses keywords as identifiers for conditions and restarts, whereas `org.sbrubbles.conditio.vars` uses vars. The end API is quite different...

Keywords:
* Require building up some machinery, but I feel it's easier to understand and inspect; the data (conditions, available handlers and restarts) are explicit.

Vars:
* Offer a very thin layer over Clojure's preexisting functionality. To the point that I question the need of a library at all...
* Seem to... complect the concepts, maybe? For example, `defcondition` basically creates a specialized handler for every condition. 

Skipping helped distinguish the approaches; I was able to add it with keywords while preserving (most of) the API. I have an idea on [how to do it with vars](https://github.com/hanjos/conditio-clj/commit/aa5ccff07ea56b0cf00015463701efd74df981fd), but it relies on Java reflection to access Clojure's innards, and I didn't see any other way. That's a complex and _fragile_ solution.

**Decision**: I'll go with keywords, and remove the vars "fork".

## 0.3.0

### vars again...

I realized my previous attempt was complicating things more than needed, and something closer to the keywords API could work well. In the end, I basically reimplemented the library with vars (including some straight up copy-and-pasting).

In the first attempt, `defcondition`/`defrestart` acted more like default handlers, set up to be "rebound" by `binding`s. Now, they are more like specialized `signal`lers, in that they signal themselves when called. So one doesn't rebind them with `binding`; instead, one uses special constructs (`handle` and `with`) to do the job.

Now that I have something (apparently!) working, there's some stuff to mull over:
* Should I go with keywords or vars? I kinda like vars a little more; the need to `def` the conditions and restarts helps documentation, and folks tend to skimp on that...
* There's very little restart code that doesn't basically reimplement dynamic variable machinery; `restart-not-found` checks, and a convenient map of available restarts in `*restarts*`. If I find a way around those, or decide they don't carry their weight, then I could remove pretty much all restart machinery (`with`, `restart`...). Hum...

