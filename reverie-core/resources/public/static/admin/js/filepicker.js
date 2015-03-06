(function(window, $) {
    $(window.document).ready(function() {
        $("span.download").each(function(){
            $(this).click(function() {
                var $span = $(this);
                var path = $span.attr("uri");
                var qs = util.query_params();
                var field = qs['field'];
                var field_name = qs['field-name'];

                if (!util.undefined_p(field)) {
                    opener[field].value = path;
                    window.close();
                }

                if (!util.undefined_p(field_name)) {
                    opener.document.getElementById(field_name).value = path;
                    window.close();
                }
            });
        });
    });
})(window, $);
