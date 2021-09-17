
const time = {
  _MONTH_NAMES: [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ],

  _zeroPad: function(n) {
    n = "00"+n;
    return n.substr(n.length - 2);
  },

  extractStrings(dt) {
    let hours = dt.getHours();
    const ampm = (hours >= 12 ? "pm" : "am");
    if (hours === 0) {
      hours = 12;
    } else if (hours > 12) {
      hours -= 12;
    }

    return {
      dt: dt.getDate() + " " + time._MONTH_NAMES[dt.getMonth()] + " " + dt.getFullYear() +
      " " + time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm,
      time: time._zeroPad(hours) + ":" + time._zeroPad(dt.getMinutes()) + " " + ampm
    }
  },

  formatTime(dt) {
    const now = new Date();
    const strs = this.extractStrings(dt);
    if (Math.abs(now.getTime() - dt.getTime()) < 5000.0) {
      // less than 5 seconds
      return "Just now";
    }

    const dtDate = new Date(dt.getFullYear(), dt.getMonth(), dt.getDate());
    const nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const days = (nowDate.getTime() - dtDate.getTime()) / (1000 * 60 * 60 * 24);
    if (Math.abs(days) < 1) {
      return "Today, " + strs.time;
    } else if (Math.abs(days) < 2) {
      if (days < 0) {
        return "Tomorrow, " + strs.time;
      } else {
        return "Yesterday, " + strs.time;
      }
    } else {
      return strs.dt;
    }
  },

  // update all <time> elements on the page to be in local time, rather than UTC
  refreshAll: function() {
    $("time[datetime], time[timestamp]").each(function() {
      let dt = null;
      if ($(this).attr("datetime")) {
        dt = new Date($(this).attr("datetime")+" UTC");
      } else if ($(this).attr("timestamp")) {
        dt = new Date(parseInt($(this).attr("timestamp")));
      } else {
        return;
      }

      const strs = this.extractStrings(dt);

      const now = new Date();
      const diff = dt.getTime() - now.getTime();

      if ($(this).hasClass("timer") && diff > 0 && diff < (24 * 60 * 60 * 1000)) {
        const diffHours = (diff / (60 * 60 * 1000)).toFixed();
        let str = "";
        if (diffHours > 0) {
          str += diffHours + " hr, ";
        }
        const diffMinutes = (diff / (60 * 1000)).toFixed() - (diffHours * 60);
        if (diffMinutes > 0) {
          str += diffMinutes + " min, ";
        }
        const diffSeconds = (diff / 1000).toFixed() - (diffMinutes * 60) - (diffHours * 60 * 60);
        str += diffSeconds + " sec";
        $(this).html(str).attr("title", strs.dt);
      } else {
        $(this).html(time.formatTime(dt)).attr("title", strs.dt);
      }
    });
  }
}

$(function() {
  time.refreshAll();
});
