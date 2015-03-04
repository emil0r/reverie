(function(window, $){

    var tree_search = $("#tree-search");
    var tree_search_form = $("#tree-search-form");
    var tree_search_icon = $("#tree-search-form #tree-search-icon");
    var icon_refresh = $(".icons i.icon-refresh");
    var icon_add_page = $(".icons i.icon-plus-sign");
    var icon_edit = $(".icons i.icon-edit-sign");
    var icon_view = $(".icons i.icon-eye-open");
    var icon_trash = $(".icons i.icon-trash");

    var icon_publish = $(".meta .buttons .publish");
    var icon_meta = $(".meta .buttons .meta");

    var meta_info_name = $(".meta .name");
    var meta_info_title = $(".meta .title");
    var meta_info_created = $(".meta .created");
    var meta_info_updated = $(".meta .updated");
    var meta_info_published_p = $(".meta .published_p");


    var show_meta_info_node = function(node) {
        meta_info_name.html(node.title);
        meta_info_title.html(node.data.page_title);
        meta_info_created.html(node.data.created);
        meta_info_updated.html(node.data.updated);
        meta_info_published_p.html(node.data.published_p === true ? "true" : "false");
    };

    var load_node = function() {
        tree.activateKey(tree_search.val());
        return false;
    };

    $("#tree").fancytree({
        source: $.ajax({
            type: "GET",
            url: "/admin/api/interface/pages",
            dataType: "json",
            complete: function() {
                $("#tree").fancytree("getTree").activateKey("1");
            }
        }),
        extensions: ["dnd"],
        lazyLoad: function(event, data) {
            data.result = $.ajax({
                url: "/admin/api/interface/pages/" + data.node.key,
                dataType: "json"
            });
        },
        click: function(event, data) {
            tree_search.val(data.node.key);
        },
        activate: function(event, data) {
            show_meta_info_node(data.node);
        }
    });

    var tree = $("#tree").fancytree("getTree");

    tree_search_icon.click(load_node);
    tree_search_form.submit(load_node);

    var get_selected_node = function() {
        var nodes = tree.getSelectedNodes();
        for (var i = 0, ii = nodes.length; i < ii; i++) {
            return nodes[i];
        }
        return null;
    };

    var add_child = function(data) {
        var node = get_selected_node();
        if (node != null && node.isExpanded()) {
            node.addChildren(data);
        }
    };

    var remove_node = function() {
        var node = get_selected_node();
        if (node != null) {
            node.remove();
        };
    };

    icon_refresh.click(function() {
        var nodes = tree.getSelectedNodes();
        for (var i = 0, ii = nodes.length; i < ii; i++) {
            nodes[i].setSelected(false);
        }
        var node = tree.getActiveNode();
        node.setSelected(true);
        dom.show_main();
        dom.main_uri(node.data.path);
    });

    icon_edit.click(function() {
        var node = get_selected_node();
        icon_view.removeClass("hidden");
        icon_edit.addClass("hidden");
        if (node !== null) {
            $.post("/admin/api/interface/pages/edit-page",
                   {
                       serial: node.key,
                       edit_p: true
                   },
                   function(data) {
                       if (data.success) {
                           reload_main();
                       } else {
                           alert(data.error);
                       }
                   },
                   "json");
        }
    });

    icon_view.click(function() {
        var node = get_selected_node();
        icon_view.addClass("hidden");
        icon_edit.removeClass("hidden");
        if (node !== null) {
            $.post("/admin/api/interface/pages/edit-page",
                   {
                       serial: node.key,
                       edit_p: false
                   },
                   function(data) {
                       if (data.success) {
                           reload_main();
                       } else {
                           alert(data.error);
                       }
                   },
                   "json");
        }
    });

    icon_add_page.click(function() {
        var node = get_selected_node();
        if (node != null) {
            dom.options_uri("/admin/api/interface/frames/pages/add/" + node.key);
            dom.show_options();
        }
    });

    icon_trash.click(function() {
        var node = get_selected_node();
        if (node != null) {
            dom.options_uri("/admin/api/interface/frames/pages/trash/" + node.key);
            dom.show_options();
        }
    });

    icon_publish.click(function() {
        var node = get_selected_node();
        if (node != null) {
            dom.options_uri("/admin/api/interface/frames/pages/publish/" + node.key);
            dom.show_options();
        }
    });


    window.tree = {selected_node: get_selected_node,
                   add_child: add_child,
                   remove_node: remove_node};
    parent.window.tree = window.tree;
})(window, jQuery);
