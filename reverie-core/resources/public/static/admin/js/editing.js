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

    var click = function(e) {
        var $item = $(this);
        var $tab = $("#" + $item.attr("tab"));
        $item.addClass("goog-tab-selected");
        $item.siblings().each(function(){
            $(this).removeClass("goog-tab-selected");
        });
        $tab.show();
        $tab.siblings().each(function() {
            var $this = $(this);
            if ($this.attr("id") != "tabbar") {
                $(this).hide();
            }
        });
    };
    $("div#tabbar > div").click(click);
})(window, jQuery);
