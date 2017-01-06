(function(window, $){

    var tree_search = $("#tree-search");
    var tree_search_form = $("#tree-search-form");
    var tree_search_icon = $("#tree-search-form #tree-search-icon");
    var icon_refresh = $(".icons i.fa-refresh");
    var icon_add_page = $(".icons i.fa-plus-circle");
    var icon_edit = $(".icons i.fa-pencil");
    var icon_view = $(".icons i.fa-eye");
    var icon_trash = $(".icons i.fa-trash");

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

    var update_children_paths = function(root) {
        var children = root.getChildren();
        if (!util.undefined_p(children) && children != null) {
            var path = root.data.path;
            for (var i = 0, ii = children.length; i < ii; i++) {
                var child = children[i];
                child.data.path = util.join_uri(path, child.data.slug);
                update_children_paths(child);
            }
        }
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
        dnd: {

            preventVoidMoves: true, // prevent node being dropped in front of itself
            preventRecursiveMoves: true, // prevent node being dropped on its own descendants
            dragStart: function(node, data) {
                /** This function MUST be defined to enable dragging for the tree.
                 *  Return false to cancel dragging of node.
                 */
                return true;
            },
            dragEnter: function(node, data) {
                /** data.otherNode may be null for non-fancytree droppables.
                 *  Return false to disallow dropping on node. In this case
                 *  dragOver and dragLeave are not called.
                 *  Return 'over', 'before, or 'after' to force a hitMode.
                 *  Return ['before', 'after'] to restrict available hitModes.
                 *  Any other return value will calc the hitMode from the cursor position.
                 */
                // Prevent dropping a parent below another parent (only sort
                // nodes under the same parent)

                /*  if(node.parent !== data.otherNode.parent){
                       return false;
                    }
                    // Don't allow dropping *over* a node (would create a child)
                    return ["before", "after"];
                 */
                return true;
            },
            dragDrop: function(node, data) {
                /** This function MUST be defined to enable dropping of items on
                 *  the tree.
                 */

                $.post("/admin/api/interface/pages/move",
                       {serial: data.otherNode.key,
                        origo_serial: node.key,
                        movement: data.hitMode},
                       function(return_data) {
                           if (!return_data.success) {
                               alert(return_data.error);
                           } else {
                               data.otherNode.moveTo(node, data.hitMode);
                               var path = data.otherNode.parent.data.path;
                               data.otherNode.data.path = util.join_uri(path, data.otherNode.data.slug);
                               update_children_paths(data.otherNode);
                           }
                       });
            }
        },
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

    var update_node = function(data) {
        var node = get_selected_node();
        if (node != null) {
            node.fromDict(data);
            show_meta_info_node(node);
        }
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

    icon_meta.click(function() {
        var node = get_selected_node();
        if (node != null) {
            dom.options_uri("/admin/api/interface/frames/pages/meta/" + node.key);
            dom.show_options();
        }
    });


    window.tree = {selected_node: get_selected_node,
                   add_child: add_child,
                   remove_node: remove_node,
                   update_node: update_node};
    parent.window.tree = window.tree;
})(window, jQuery);
