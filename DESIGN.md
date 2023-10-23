# Some thoughts on design decisions and speculations

## vars vs keywords

`org.sbrubbles.conditio` uses keywords as identifiers for conditions and restarts, whereas `org.sbrubbles.conditio.vars` uses vars. The end API is quite different, and I'm not sure which I prefer...

Using keywords:
* Requires building up some machinery, but I feel demonstrates the concept of conditions better.

Using vars:
* Takes a lot of advantage of Clojure's preexisting functionality; so much the end result is effectively two macros, a condition and a function (`v/bind-fn`) that one could argue should be in Clojure's API directly.  

