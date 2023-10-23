# Thoughts and speculations on design

## 0.2.0-SNAPSHOT

### vars vs keywords

`org.sbrubbles.conditio` uses keywords as identifiers for conditions and restarts, whereas `org.sbrubbles.conditio.vars` uses vars. The end API is quite different, and I'm not sure which I prefer...

Using keywords:
* Requires building up some machinery, but I feel it's easier to understand and inspect; the data (conditions, available handlers and restarts) are explicit.

Using vars:
* It's a very thin layer over Clojure's preexisting functionality; just a couple of helper macros (and function). That minimalism is very appealing.
* The concepts seem more... blurred together? For example, condition and signal are blurred together in defcondition, which effectively creates a specialized signal function.


