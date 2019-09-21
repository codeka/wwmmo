// update all <time> elements on the page to be in local time, rather than UTC
function fix_times() {
  function zero_pad(n) {
    n = "00"+n;
    return n.substr(n.length - 2);
  }

  var month_names = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  $("time[datetime], time[timestamp]").each(function() {
    var dt = null;
    if ($(this).attr("datetime")) {
      dt = new Date($(this).attr("datetime")+" UTC");
    } else {
      dt = new Date($(this).attr("timestamp") * 1000);
    }

    var hours = dt.getHours();
    var ampm = (hours >= 12 ? "pm" : "am");
    if (hours == 0) {
      hours = 12;
    } else if (hours > 12) {
      hours -= 12;
    }

    var dtstr = dt.getDate()+" "+month_names[dt.getMonth()]+" "+dt.getFullYear()+" "+zero_pad(hours)+":"+zero_pad(dt.getMinutes())+" "+ampm;

    var now = new Date();
    var seconds = (now.getTime() - dt.getTime()) / 1000.0;
    if (seconds < (5 * 60) && seconds > 0) {
      // less than five minutes ago
      $(this).html("Just now")
             .attr("title", dtstr);
    } else {
      var dtDate = new Date(dt.getFullYear(), dt.getMonth(), dt.getDate());
      var nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      var timestr = zero_pad(hours)+":"+zero_pad(dt.getMinutes())+" "+ampm;
      var days = (nowDate.getTime() - dtDate.getTime()) / (1000 * 60 * 60 * 24);
      if (days == 0) {
        $(this).html("Today, "+timestr)
               .attr("title", dtstr);
      } else if (days == 1) {
        $(this).html("Yesterday, "+timestr)
               .attr("title", dtstr);
      } else if (days == -1) {
        $(this).html("Tomorrow, "+timestr)
               .attr("title", dtstr);
      } else {
        $(this).html(dtstr);
      }
    }
  });
}

$(function() {
  fix_times();
});
