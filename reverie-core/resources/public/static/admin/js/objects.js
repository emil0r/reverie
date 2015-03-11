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
            var area = $(this).attr("area");
            var exists_p = false;
            for (var i, ii = areas.length; i < ii; i++) {
                if (area === areas[i]) {
                    exists_p = true;
                }
            }
            if (!exists_p) {
                areas.push(area);
            }
        });

        dom.$m(".reverie-area").each(function() {
            var area = $(this).attr("area");
            $(this).find(".move-object-to .reverie-bar").each(function() {
                if (areas.length === 0) {
                    $(this).remove();
                } else {
                    var $areas = $(this).parents(".reverie-area");
                    var html = "";
                    for (var i = 0, ii = areas.length; i < ii; i++) {
                        if (areas[i] !== area) {
                            html += "<li action='move-to-area' area='" + areas[i] + "'>area " + areas[i] + "</li>";
                        }
                    }
                    // in the event of areas under areas we check that we are under the immediate area
                    if ($areas.length > 0 && $($areas[0]).attr("area") === area) {
                        $(this).before(html);
                    }
                }
            });
        });

        dom.$m(".reverie-object li").on('click', function() {
            var action = $(this).attr('action');
            if (typeof action != 'undefined'
                && action !== ''
                && action.length > 0) {
                var object_id = $(this).parents(".reverie-object").attr("object-id");
                var url = null;
                var data = {};

                switch (action) {

                case 'edit':
                    window.open("/admin/api/interface/frames/object/" + object_id,
                                '_blank',
                                'fullscreen=no, width=400, height=640');
                    break;
                case 'delete':
                    url = '/admin/api/interface/objects/delete';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key};
                    break;
                case 'move-top':
                    url = '/admin/api/interface/objects/move';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key,
                            direction: 'top'};
                    break;
                case 'move-up':
                    url = '/admin/api/interface/objects/move';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key,
                            direction: 'up'};
                    break;
                case 'move-down':
                    url = '/admin/api/interface/objects/move';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key,
                            direction: 'down'};
                    break;
                case 'move-bottom':
                    url = '/admin/api/interface/objects/move';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key,
                            direction: 'bottom'};
                    break;
                case 'move-to-area':
                    url = '/admin/api/interface/objects/move-to-area';
                    data = {object_id: object_id,
                            page_serial: tree.selected_node().key,
                            area: $(this).attr('area')};
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
