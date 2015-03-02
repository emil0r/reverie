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

        dom.$m(".reverie-area").each(function() {
            var area = $(this).attr("area");
            $(this).find(".move-object-to .reverie-bar").each(function() {
                if (areas.length === 0) {
                    $(this).remove();
                } else {
                    var html = "";
                    for (var i = 0, ii = areas.length; i < ii; i++) {
                        if (areas[i] !== area) {
                            html += "<li action='move-to-area' area='" + areas[i] + "'>area " + areas[i] + "</li>";
                        }
                    }
                    $(this).before(html);
                }
            });
        });

        dom.$m(".reverie-object li").on('click', function() {
            var action = $(this).attr('action');
            if (action !== '' && action.length > 0) {
                var object_id = $(this).parents(".reverie-object").attr("object-id");
                var url = null;
                var data = {};

                switch (action) {
                    case 'edit':
                    break;
                    case 'delete':
                    url = '/admin/api/interface/objects/delete';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key};
                    break;
                }
                if (url !== null) {
                    $.post(url, data, function(data) {
                        if (!data.success) {
                            alert(data.error);
                        } else {
                            dom.reload_main();
                        }
                    });
                };
            }
        });

        dom.$m(".reverie-area .add-objects li").on('click', function() {
            var object = $(this).html();
            if (object !== '' && object.length > 0) {
                var area = $(this).parents(".reverie-area").attr("area");
                var url = null;
                var data = {};

                $.post('/admin/api/interface/objects/add',
                       {area: area,
                        object: object,
                        page_serial: tree.selected_node().key},
                       function(data) {
                           if (!data.success) {
                               alert(data.error);
                           } else {
                               dom.reload_main();
                           }
                       });
            }
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
