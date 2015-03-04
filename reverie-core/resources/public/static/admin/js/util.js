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
    window.util = {slugify: slugify};
})(window, jQuery);
