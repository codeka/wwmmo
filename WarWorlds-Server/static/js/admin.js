
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

    $(document).ajaxSend(function(evnt, xhr) {
        xhr.startTime = new Date();
    });

    $(document).ajaxSuccess(function(evnt, xhr) {
        xhr.endTime = new Date();
        if (xhr.startTime) {
          xhr.elapsedMs = (xhr.endTime.getTime() - xhr.startTime.getTime());
        } else {
          xhr.elapsedMs = 0;
        }
    });

    // TODO: store "on_behalf_of" in localStorage or something
});
