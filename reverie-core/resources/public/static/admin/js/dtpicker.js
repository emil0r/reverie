$(document).ready(function(){
    $("input[type=_datetime]").each(function(){
        $(this).appendDtpicker({isInline: false,
                                closeOnSelected: true});
    });
});
