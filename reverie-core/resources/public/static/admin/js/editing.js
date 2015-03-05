(function(window, $){
    $("input[_type='slug']").each(function() {
        var $watch = $("input[name='" + $(this).attr("for") + "']");
        var $slug = $(this);
        var watch_val = $watch.val();
        var slug_val = $slug.val();

        // only initiate watching if the slug and the watch has the same value
        if (watch_val === slug_val) {

            var watch_fn = function() {
                $slug.val(util.slugify($watch.val()));
            };

            var slug_fn = function() {
                $slug.val(util.slugify($slug.val()));
            };

            $watch.keyup(watch_fn);
            $watch.change(watch_fn);

            $slug.change(slug_fn);
        }
    });
})(window, jQuery);
