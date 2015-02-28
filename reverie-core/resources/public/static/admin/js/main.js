$(document).ready(function(){
    tabs.init();
    $(".logout").click(function(){
        window.parent.location = "/admin/logout";
    });
});
