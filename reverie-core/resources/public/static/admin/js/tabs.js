(function(window) {
    var $tabbar = $("#tabbar");
    var $navigation = $("#tabbar [tab='navigation-meta']");
    var $modules = $("#tabbar [tab='modules']");

    var click = function() {
        var $tab = $(this);
        $tab.addClass("goog-tab-selected");
        $tab.siblings().each(function(){
            $(this).removeClass("goog-tab-selected");
        });
    };

    var click_module = function() {
        var $module = $(this);
        $module.addClass("active");
        $module.siblings().removeClass("active");

        dom.options_uri("/admin/frame/module/" + $module.attr("module"));
        dom.show_options();
    };

    var show_modules = function() {
        $("div.modules").removeClass("hidden");
        $("div.navigation-meta").addClass("hidden");
    };
    var hide_modules = function() {
        $("div.modules").addClass("hidden");
        $("div.modules > ul > li").each(function(){
            $(this).removeClass("active");
        });
        $("div.navigation-meta").removeClass("hidden");
    };

    var init = function() {
        $navigation.click(click);
        $navigation.click(hide_modules);
        $modules.click(click);
        $modules.click(show_modules);
        $("div.modules > ul > li").each(function(){
            $(this).click(click_module);
        });
    };

    window.tabs = {init: init};
})(window);
