(function(window, $){
    var init_main = function() {
        dom.$m("html").each(function(){
            $(this).on('click', function() {
                dom.$m(".reverie-object-menu").each(function(){
                    $(this).addClass("hidden");
                });
                dom.$m(".reverie-area-menu").each(function(){
                    $(this).addClass("hidden");
                });
            });
        });

        var areas = [];
        dom.$m(".reverie-area").each(function() {
            areas.push($(this).attr("area"));
        });

        dom.$m(".reverie-area-panel").on('click', function(e) {
            $(this).siblings(".reverie-area-menu").removeClass("hidden");
            e.stopPropagation();
        });

        dom.$m(".reverie-object-panel").on('click', function(e) {
            $(this).find(".reverie-object-menu").removeClass("hidden");
            e.stopPropagation();
        });
    };
    dom.$m_ready(init_main);
    window.objects = {init_main: init_main};
})(window, jQuery);
