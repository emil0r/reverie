var getParameterByName = function (name) {
  name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
  results = regex.exec(location.search);

  return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}


// file browser callback. see tinymce documentation about it
var fbc = function(field_name, url, type, win){
  var base = type === "image" ? 
    "/admin/frame/filemanager/images" :
    "/admin/frame/url-picker";
     
  window.open(base
              + "?field-name=" + field_name
              + "&url=" + url,
              "_blank",
              "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1",
              true);
};

$(document).ready(function(){
  tinymce.init({
    selector: "textarea",
    height: 450,
    width: 798,
    plugins: ["link image charmap contextmenu table"],
    toolbar: "insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image",
    file_browser_callback: fbc
  });

  $("#save").click(function(){
    var field = getParameterByName("field");
    var form = getParameterByName("form");
    var text = tinyMCE.activeEditor.getContent();

    opener.document[form][field].value = text;
    window.close();
  });
  $("cancel").click(function(){
    window.close();
  });
});

