var getParameterByName = function (name) {
  name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
  results = regex.exec(location.search);

  return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

$(document).ready(function(){
  tinymce.init({
    selector: "textarea",
    height: 400,
    plugins: ["link image"],
    toolbar: "insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image"
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

