# Thoughts and speculations on design

## 0.2.0-SNAPSHOT

### vars vs keywords

`org.sbrubbles.conditio` uses keywords as identifiers for conditions and restarts, whereas `org.sbrubbles.conditio.vars` uses vars. The end API is quite different...

Using keywords:
* Requires building up some machinery, but I feel it's easier to understand and inspect; the data (conditions, available handlers and restarts) are explicit.

Using vars:
* It's a very thin layer over Clojure's preexisting functionality; just a couple of helper macros (and function). That minimalism is very appealing, but it's almost to the point that I question the need of a library at all...
* The concepts seem... complected, maybe? For example, condition and signal are blurred together in defcondition, which effectively creates a specialized signal function for that condition.

Strictly speaking, both approaches are very simplistic; none have a notion of skipping, for example. Maybe adding more features would help distinguish them...


