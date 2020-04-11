var currentEmpire = null;

$("#find-byemailidname").on("submit", function(evnt) {
  evnt.preventDefault();

  var query;
  var $inp = $("#search-value");
  if ($inp.val().indexOf("@") > 0) {
    query = "email="+$inp.val();
  } else if (parseInt($inp.val()) == $inp.val()) {
    query = "ids="+$inp.val();
  } else {
    query = "name="+$inp.val();
  }
  fetchEmpireList(query);
});

$("#refresh-ranks input").click(function() {
  $.ajax({
    url: "refresh-ranks",
    type: "POST",
    success: function(data) {
      $("#find-byemailidname input[type=submit]").click();
    },
    error: function(xhr, status, err) {
      alert("An error occured, check server logs: " + xhr.status);
    }
  });
});

function zeroPad(str, n) {
  var padded = "0000000000"+str;
  return padded.substr(padded.length - n);
}
function spacePad(str, n) {
  var padded = "                                           "+str;
  return padded.substr(padded.length - n);
}

$("#empire-query").on("click", ":submit", function(evnt) {
  evnt.preventDefault();
  if($(this).val() == "Cash Audit") {
    var url = "/realms/{{realm}}/empires/"+$("#empire-key").val()+"/cash-audit";
    $.ajax({
      url: url,
      type: "GET",
      dataType: "json",
      cache: false,
      success: function(data) {
        var text = "";
        text += spacePad("Date", 19);
        text += " | "+spacePad("Before", 10);
        text += " | "+spacePad("After", 10);
        text += " | "+spacePad("Reason", 20);
        text += "\r\n";
        for(var i = 0; i < data.records.length; i++) {
          var entry = data.records[i];
          var dt = new Date(entry["time"] * 1000);
          text += zeroPad(dt.getFullYear(), 4)+"-"+zeroPad(dt.getMonth()+1, 2)+"-"+zeroPad(dt.getDate(), 2);
          text += " "+zeroPad(dt.getHours(), 2)+":"+zeroPad(dt.getMinutes(), 2)+":"+zeroPad(dt.getSeconds(), 2);
          text += " | "+spacePad(parseInt(entry["before_cash"]), 10);
          text += " | "+spacePad(parseInt(entry["after_cash"]), 10);
          var reason = entry["reason"];
          if (!reason) {
            if (entry["accelerate_amount"]) {
              reason = "AccelerateBuild";
            } else if (entry["move_distance"]) {
              reason = "FleetMove";
            }
          }
          text += " | "+spacePad(reason, 20);
          if (reason == "FleetMove") {
            text += " | fleet_id="+entry["fleet_id"];
            text += " design_id="+entry["fleet_design_id"];
            text += " x "+entry["num_ships"];
            text += " distance="+entry["move_distance"];
            text += " destination="+entry["star_name"]+" "+entry["star_id"];
          } else if (reason == "AccelerateBuild") {
            text += " | design_id="+entry["build_design_id"];
            text += " x "+entry["build_count"];
            text += " accelerate_amount="+parseInt(entry["accelerate_amount"] * 100.0)+"%";
          }
          text += "\r\n";
        }
        $("#search-results").addClass("code").html(text);
      },
      error: function(xhr, status, err) {
        alert("An error occured, check server logs: " + xhr.status);
      }
    });
  } else {
    var url = "/realms/{{realm}}/empires/"+$("#empire-key").val()+"/building-statistics";
    $.ajax({
      url: url,
      type: "GET",
      dataType: "json",
      success: function(data) {
        $("#search-results").addClass("code").html(JSON.stringify(data, null, "  "));
      },
      error: function(xhr, status, err) {
        alert("An error occurred, check server logs: " + xhr.status);
      }
    });
  }
});

$("#empire-list").on("click", "a.empire-name", function() {
  fetchEmpire($(this).data("id"));
});

function fetchEmpire(id) {
  $.ajax({
    url: "/realms/{{realm}}/empires/search?ids="+id,
    dataType: "json",
    cache: false,
    success: function (data, status, xhr) {
      setTimeout(function() {
        $("#search-time").html("<b>Time:</b> "+xhr.elapsedMs+"ms");
      }, 10);
      var tmpl = $("#empire-details");
      var emp = data.empires[0];
      if (!emp["home_star"]) {
        emp.home_star = null;
      }
      if (!emp["alliance"]) {
        emp.alliance = null;
      }
      currentEmpire = emp;
      $("#search-results").removeClass("code").html(tmpl.applyTemplate(emp));
    },
    error: function(xhr, status, err) {
      if (xhr.status == 404) {
        $("#search-results").val("No empire!");
      } else {
        alert("An error occurred, check server logs: " + xhr.status);
      }
    }
  });
}

function fetchEmpireList(query) {
  var $tbody = $("#empire-list tbody");
  $tbody.html("<tr><td colspan=\"4\"><div class=\"spinner\"></div></td></tr>");

  $.ajax({
      url: "/realms/{{realm}}/empires/search?"+query,
      dataType: "json",
      success: function(data) {
        showEmpireList(data.empires);
      }
    });
}

function showEmpireList(empires) {
  var rowTmpl = $("#empire-row");
  var $tbody = $("#empire-list tbody");
  $tbody.empty();
  for (var i = 0; i < empires.length; i++) {
    var empire = empires[i];
    $tbody.append(rowTmpl.applyTemplate(empire));
  }
}

$(function() {
  hashparams.register(function(params) {
    if ($("#search-value").val() != params["search"]) {
      $("#search-value").val(params["search"]);
      $("#find-byemailidname").submit();
    }

    if (params["id"]) {
      fetchEmpire(params["id"]);
    }
  });

  if ($("#search-value").val() == "") {
    fetchEmpireList("minRank=1&maxRank=50");
  }
});

$("body").on("click", ".empire-logins-fetch", function() {
  if (currentEmpire == null) {
    return;
  }

  $.ajax({
      url: "/realms/{{realm}}/admin/empire/" + currentEmpire.key + "/logins",
      success: function(data) {
        $("#empire-logins-tab").html(data);
      }
  });
});

$("body").on("click", ".empire-sitrep-fetch", function() {
  if (currentEmpire == null) {
    return;
  }

  $.ajax({
       url: "/realms/{{realm}}/sit-reports?empireId=" + currentEmpire.key,
       dataType: "json",
       success: function(data) {
         $("#empire-sitrep-tab").html("<code><pre>" + JSON.stringify(data, null, "  ") + "</pre></code>");
       }
  });
});