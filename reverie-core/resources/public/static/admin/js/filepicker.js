(function(window, $) {
    $(window.document).ready(function() {
        $("span.download").each(function(){
            $(this).click(function() {
                var $span = $(this);
                var path = $span.attr("path");
                var field = util.query_params()['field'];
                opener[field].value = path;
                window.close();
            });
        });
    });
})(window, $);
