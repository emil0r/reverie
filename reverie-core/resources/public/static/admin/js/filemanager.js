(function(window, $){
    $(window.document).ready(function(){
        var $delete = $("input[name='delete']");
        $("span.file").click(function(){
            var $file = $(this);
            $file.siblings("span.file").removeClass("selected");
            $file.addClass("selected");
            $delete.val($file.attr("uri"));
        });
    });
})(window, jQuery);
