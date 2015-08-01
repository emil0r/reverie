(function(window, $){
    // file browser callback. see tinymce documentation about it
    var fbc = function(field_name, url, type, win){
        var base = type === "image" ?
                "/admin/frame/filepicker" :
                "/admin/api/interface/frames/url-picker";

        window.open(base
                    + "?field-name=" + field_name,
                    "_blank",
                    "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1",
                    true);
    };
    var styles = [
        {title: 'Headers', items: [
            {title: 'Header 1', format: 'h1'},
            {title: 'Header 2', format: 'h2'},
            {title: 'Header 3', format: 'h3'},
            {title: 'Header 4', format: 'h4'},
            {title: 'Header 5', format: 'h5'},
            {title: 'Header 6', format: 'h6'}
        ]},

        {title: 'Inline', items: [
            {title: 'Bold', icon: 'bold', format: 'bold'},
            {title: 'Italic', icon: 'italic', format: 'italic'},
            {title: 'Underline', icon: 'underline', format: 'underline'},
            {title: 'Strikethrough', icon: 'strikethrough', format: 'strikethrough'},
            {title: 'Superscript', icon: 'superscript', format: 'superscript'},
            {title: 'Subscript', icon: 'subscript', format: 'subscript'},
            {title: 'Code', icon: 'code', format: 'code'}
        ]},

        {title: 'Blocks', items: [
            {title: 'Paragraph', format: 'p'},
            {title: 'Blockquote', format: 'blockquote'},
            {title: 'Div', format: 'div'},
            {title: 'Pre', format: 'pre'}
        ]},

        {title: 'Alignment', items: [
            {title: 'Left', icon: 'alignleft', format: 'alignleft'},
            {title: 'Center', icon: 'aligncenter', format: 'aligncenter'},
            {title: 'Right', icon: 'alignright', format: 'alignright'},
            {title: 'Justify', icon: 'alignjustify', format: 'alignjustify'}
        ]}
            ||extra-formats||
    ];

    var selector = "#|selector|";
    tinymce.init({
        selector: selector,
        convert_urls: false,
        plugins: ["link image charmap contextmenu table code"],
        toolbar: "insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image",
        style_formats: styles,
        file_browser_callback: fbc
    });
})(window, jQuery);
