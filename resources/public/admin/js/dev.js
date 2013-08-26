goog.require("reverie.core");
goog.require("reverie.dev");

$(document).ready(function() {
  reverie.dev.start_repl();
  reverie.core.init();
});

