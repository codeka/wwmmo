google.load("visualization", "1", {packages:["corechart"]});

var currentEmpireID = null;

$("#search").on("submit", function(evnt) {
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
      $("#search input[type=submit]").click();
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
   currentEmpireID = $(this).data("id");
   $("#search-results")
       .removeClass("code")
       .html($("#empire-details").applyTemplate({}));
   tabs.refresh();
});

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

  // If there's only one, just click on it since that's probably what you want anyway.
  if (empires.length == 1) {
    $("#empire-list a.empire-name").click();
  }
}

// Renders the given request graph (given in data) to the given div.
function renderRequestGraph(div, data) {
  // keep the width/height ratio of the graph nice
  var ratio = 0.333;
  $(div).css("height", $(div).width() * ratio);

  var dataTable = new google.visualization.DataTable();
  dataTable.addColumn("datetime", "Date");
  dataTable.addColumn("number", "Total requests");
  dataTable.addColumn("number", "{{empire.displayName}} Total");
  dataTable.addColumn("number", "Not rate limited");
  dataTable.addColumn("number", "Soft rate limited");
  dataTable.addColumn("number", "Hard rate limited");
  dataTable.addRows(data);

  var dataView = new google.visualization.DataView(dataTable);

  var width = $(div).width();
  var height = $(div).height();
  var options = {
    "chartArea": {left: 50, top: 10, width: width - 200, height: height - 80},
    "backgroundColor": {fill: "transparent"}
  };

  var chart = new google.visualization.LineChart(div);
  chart.draw(dataView, options);
}

$(function() {
  hashparams.register(function(params) {
    if ($("#search-value").val() != params["search"]) {
      $("#search-value").val(params["search"]);
      $("#search").submit();
    }

    if (params["id"]) {
      fetchEmpire(params["id"]);
    }
  });

  if ($("#search-value").val() == "") {
    fetchEmpireList("minRank=1&maxRank=50");
  }
});

$("body").on("tab:show", "#empire-logins-tab", function() {
  if (currentEmpireID == null) {
    return;
  }

  $.ajax({
      url: "/realms/{{realm}}/admin/empire/" + currentEmpireID + "/logins",
      success: function(data) {
        $("#empire-logins-tab").html(data);
      }
  });
});

$("body").on("tab:show", "#empire-sitrep-tab", function() {
  if (currentEmpireID == null) {
    return;
  }

  $.ajax({
       url: "/realms/{{realm}}/sit-reports?empireId=" + currentEmpireID,
       dataType: "json",
       success: function(data) {
         $("#empire-sitrep-tab").html("<code><pre>" + JSON.stringify(data, null, "  ") + "</pre></code>");
       }
  });
});

$("body").on("tab:show", "#empire-details-tab", function() {
  if (currentEmpireID == null) {
    return;
  }

  $.ajax({
       url: "/realms/{{realm}}/admin/empire/" + currentEmpireID + "/details",
       success: function(data) {
         $("#empire-details-tab").html(data);
       }
  });
});