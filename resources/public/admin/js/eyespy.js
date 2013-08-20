(function(window)
{
    var eyespy = 
        {
            log: function(msg) {
                if (console && console.log) {
                    console.log(msg);
                }
            },
            random: function() {
                return Math.floor(Math.random() * 999999999);
            },
            init: function(address, split_by) {
                if (typeof(address) == 'undefined') {
                    address = "ws://127.0.0.1:4321/";
                }
                if (typeof(split_by) == 'undefined') {
                    split_by = '/';
                }
                if ("WebSocket" in window) {
                    var ws = new WebSocket("ws://127.0.0.1:4321/");
                    ws.onopen = function() {
                        ws.send("Connect...");
                    };
                    ws.onmessage = function (evt) {
                        var msg = evt.data;
                        if (msg == "Connected...") {
                            eyespy.log("We are connected...");
                            return true;
                        }
                        var links = document.getElementsByTagName("link");
                        var split_msg = msg.split(split_by);
                        var changed_stylesheet = split_msg[split_msg.length-1];
                        
                        for (var i = 0, ii = links.length; i < ii; i++) {
                            var link = links[i];
                            if (link.rel == "stylesheet") {
                                var split_href = link.href.split("/");
                                var href_stylesheet = split_href[split_href.length-1];
                                href_stylesheet = href_stylesheet.split("?")[0];
                                if (changed_stylesheet === href_stylesheet) {
                                    link.href = link.href.split("?")[0] + "?" + eyespy.random();
                                }
                            }
                        }
                        return true;
                    };
                    ws.onclose = function() {
                        // websocket is closed
                    };
                } else {
                    eyespy.log('WebSockets not supported. EyeSpy will not work');
                }
            }
        };
    window.eyespy = eyespy;
    
})(window);