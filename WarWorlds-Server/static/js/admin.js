
$(function() {
    // if you populate the "Impersonate" field, then all requests get the
    // "on_behalf_of" query parameter added.
    $.ajaxPrefilter(function(options, originalOptions, jqXHR) {
        var onBehalfOf = $("#on_behalf_of").val();
        if (onBehalfOf != "") {
            if (options.url.indexOf("?") >= 0) {
                options.url += "&";
            } else {
                options.url += "?";
            }
            options.url += "on_behalf_of="+onBehalfOf;
        }
    });

    // TODO: store "on_behalf_of" in localStorage or something
});
