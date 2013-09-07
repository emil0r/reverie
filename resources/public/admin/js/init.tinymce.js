var getParameterByName = function (name) {
  name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
  results = regex.exec(location.search);

  return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

$(document).ready(function(){
  tinymce.init({
    selector: "textarea",
    height: 400
  });

  $("#save").click(function(){
    var field = getParameterByName("field");
    var text = tinyMCE.activeEditor.getContent();

    opener.document.form_object[field].value = text;
    window.close();
  });
  $("cancel").click(function(){
    window.close();
  });
});

