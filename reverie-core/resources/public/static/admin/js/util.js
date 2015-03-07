(function(window, $) {
    var slugify = function(str) {
        return str.replace(/[åÅäÄĀāĀāÀÁÂÃÆàáâãæ]/g, 'a').
            replace(/[ČčÇç]/g, 'c').
            replace(/[Ðð]/g, 'd').
            replace(/[ĒēĒēËëÈÉÊËèéêë]/g, "e").
            replace(/[Ğğ]/g, "g").
            replace(/[ĪīĪīÏïİıìíîïÌÍÎÏ]/g, "i").
            replace(/[Ĳĳ]/g, "ij").
            replace(/[Ññ]/g, "n").
            replace(/[öÖŐőŌōŌōŒœŒœòóôõöøÒÓÔÕÖØ]/g, "o").
            replace(/[Þþ]/g, "p").
            replace(/[Řř]/g, "r").
            replace(/[ŠšŠšŠŞşŠš]/g, "s").
            replace(/[ß]/g, "ss").
            replace(/[ŰűŪūŪūÜüÙÚÛÜùúûü]/g, "u").
            replace(/[ẀẁẂẃŴŵ]/g, "w").
            replace(/[ŶŷŸýÝÿŸ]/g, "y").
            replace(/[ŽžŽžŽžžŽ]/g, "z").
            replace(/\s/g, "-").
            replace(/\&/g, "-").
            replace(/\./g, "-").
            replace(/:/g, "-").
            replace(/[^a-zA-Z0-9\-\_\.]/g, "").
            replace(/^-/, "").
            replace(/-$/, "").
            replace(/-{2,}/g, '-').
            toLowerCase();
    };

    var query_params = function() {
        var search = location.search.replace('?', '');
        var terms = search.split('&');
        var params = {};
        for (var i = 0, ii = terms.length; i < ii; i++) {
            var subparams = terms[i].split('=');
            if (subparams.length == 2) {
                params[subparams[0]] = subparams[1];
            } else if (subparams.length == 1) {
                params[subparams[0]] = "";
            }
        }
        return params;
    };

    var undefined_p = function(value) {
        return typeof value === 'undefined';
    };

    var join_uri = function() {
        var parts = [];
        for (var i = 0, ii = arguments.length; i < ii; i++) {
            var path_parts = arguments[i].split('/');
            for (var j = 0, jj = path_parts.length; j < jj; j++) {
                var part = path_parts[j];
                if (!undefined_p(part) && part !== '' && part !== '/') {
                    parts.push(part);
                }
            }
        }

        return '/' + parts.join('/');
    };

    window.util = {slugify: slugify,
                   query_params: query_params,
                   undefined_p: undefined_p,
                   join_uri: join_uri};
})(window, jQuery);
