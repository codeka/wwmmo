
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

    // save on_behalf_of in localStorage
    $(window).on("beforeunload", function() {
        window.localStorage.setItem("on_behalf_of", $("#on_behalf_of").val());
    });
    $("#on_behalf_of").val(window.localStorage.getItem("on_behalf_of"));

    // clicking the toggle button toggles the menu
    $("#navmenu-toggle").on("click", function() {
        var $navmenu = $("#navmenu");
        var show = ($navmenu.css("display") == "none");

        $navmenu.show();
        $navmenu.animate({
                "width": (show ? "300" : "0")+"px"
            }, "fast", function() {
                if (!show) {
                    $navmenu.hide();
                }
            });
        $("#maincontent").animate({
                "left": (show ? "310" : "0")+"px"
            }, "fast");
    });
});

// helper to format a number with comma thousand separators.
function formatNumber(n) {
  return (1 * n).toLocaleString({"useGrouping": true});
}
