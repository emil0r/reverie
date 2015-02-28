(function(window){

    var tree_search = $("#tree-search");
    var tree_search_form = $("#tree-search-form");
    var tree_search_icon = $("#tree-search-form #tree-search-icon");
    var icon_refresh = $(".icons i.icon-refresh");
    var icon_add_page = $(".icons i.icon-plus-sign");
    var icon_edit = $(".icons i.icon-edit-sign");
    var icon_trash = $(".icons i.icon-trash");

    $("#tree").fancytree({
        source: {
            url: "/admin/api/interface/pages"
        },
        extensions: ["dnd"],
        lazyLoad: function(event, data) {
            data.result = $.ajax({
                url: "/admin/api/interface/pages/" + data.node.key,
                dataType: "json"
            });
        },
        click: function(event, data) {
            tree_search.val(data.node.key);
        }
    });

    var tree = $("#tree").fancytree("getTree");

    var load_node = function() {
        tree.activateKey(tree_search.val());
        return false;
    };

    tree_search_icon.click(load_node);
    tree_search_form.submit(load_node);

    icon_refresh.click(function() {
        var node = tree.getActiveNode();
        dom.main_uri(node.data.path);
    });

    //

    window.tree = {};
})(window);
