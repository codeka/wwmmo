
$(function() {
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

  // clicking the toggle button toggles the menu
  $("#navmenu-toggle").on("click", function() {
    var $navmenu = $("#navmenu");
    var show = ($navmenu.css("display") == "none");
    localStorage["navmenu.hidden"] = !show;

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
  var isHidden = localStorage["navmenu.hidden"] == "true";
  if (isHidden) {
    $("#navmenu").hide();
    $("#maincontent").css("left", "0px");
  }
});

// helper to format a number with comma thousand separators.
function formatNumber(n) {
  return (1 * n).toLocaleString({"useGrouping": true});
}

// helper to convert a string to title case.
function toTitleCase(str) {
  return str.replace(/\w\S*/g, function(txt) {
    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
  });
}

// Simple jQuery plugin for working with the query string.
// Stolen without shame from http://stackoverflow.com/a/3855394/241462
(function($) {
  $.QueryString = (function(a) {
    if (a == "") return {};
    var b = {};
    for (var i = 0; i < a.length; ++i) {
      var p=a[i].split('=', 2);
      if (p.length != 2) continue;
      b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
    }
    return b;
  })(window.location.search.substr(1).split('&'))
})(jQuery);
