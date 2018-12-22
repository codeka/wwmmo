
var time = {

  _MONTH_NAMES: [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ],

  _zeroPad: function(n) {
    n = "00"+n;
    return n.substr(n.length - 2);
  },

  formatTime(dt) {
    var now = new Date();
    var hours = dt.getHours();
    var ampm = (hours >= 12 ? "pm" : "am");
    if (hours == 0) {
      hours = 12;
    } else if (hours > 12) {
      hours -= 12;
    }

    var dtstr = dt.getDate() + " " + time._MONTH_NAMES[dt.getMonth()] + " " + dt.getFullYear() +
        " " + time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm;
    var timestr = time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm;

    if (Math.abs(now.getTime() - dt.getTime()) < 5000.0) {
      // less than 5 seconds
      return "Just now";
    }

    var dtDate = new Date(dt.getFullYear(), dt.getMonth(), dt.getDate());
    var nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    var days = (nowDate.getTime() - dtDate.getTime()) / (1000 * 60 * 60 * 24);
    if (Math.abs(days) < 1) {
      return "Today, " + timestr;
    } else if (Math.abs(days) < 2) {
      if (days < 0) {
        return "Tomorrow, " + timestr;
      } else {
        return "Yesterday, " + timestr;
      }
    } else {
      return dtstr;
    }
  },

  // update all <time> elements on the page to be in local time, rather than UTC
  refreshAll: function() {
    $("time[datetime], time[timestamp]").each(function() {
      var dt = null;
      if ($(this).attr("datetime")) {
        dt = new Date($(this).attr("datetime")+" UTC");
      } else {
        dt = new Date(parseInt($(this).attr("timestamp")));
      }

      var hours = dt.getHours();
      var ampm = (hours >= 12 ? "pm" : "am");
      if (hours == 0) {
        hours = 12;
      } else if (hours > 12) {
        hours -= 12;
      }

      var dtstr = dt.getDate() + " " + time._MONTH_NAMES[dt.getMonth()] + " " + dt.getFullYear() +
          " " + time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm;
      var timestr = time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm;

      var now = new Date();
      var diff = dt.getTime() - now.getTime();

      if ($(this).hasClass("timer") && diff > 0 && diff < (24 * 60 * 60 * 1000)) {
        var diffHours = parseInt(diff / (60 * 60 * 1000));
        var str = "";
        if (diffHours > 0) {
          str += diffHours + " hr, ";
        }
        var diffMinutes = parseInt(diff / (60 * 1000)) - (diffHours * 60);
        if (diffMinutes > 0) {
          str += diffMinutes + " min, ";
        }
        var diffSeconds = parseInt(diff / 1000) - (diffMinutes * 60) - (diffHours * 60 * 60);
        str += diffSeconds + " sec";
        $(this).html(str)
            .attr("title", dtstr);
      } else {
        $(this).html(time.formatTime(dt)).attr("title", dtstr);
      }
    });
  }
}

$(function() {
  time.refreshAll();
});
