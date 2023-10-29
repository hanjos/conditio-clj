# Thoughts and speculations on design

## 0.2.0-SNAPSHOT

### vars vs keywords

`org.sbrubbles.conditio` uses keywords as identifiers for conditions and restarts, whereas `org.sbrubbles.conditio.vars` uses vars. The end API is quite different...

Keywords:
* Require building up some machinery, but I feel it's easier to understand and inspect; the data (conditions, available handlers and restarts) are explicit.

Vars:
* Offer a very thin layer over Clojure's preexisting functionality. To the point that I question the need of a library at all...
* Seem to... complect the concepts, maybe? For example, `defcondition`  effectively creates a specialized signal function for every condition.

Maybe adding more features would help to distinguish them. Skipping, for example, was implemented in keywords while preserving (most of) the API. I haven't figured to how do it in vars yet; at least not without extra functions... 


