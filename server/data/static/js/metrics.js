/**
 * Metrics is what we use to display metrics.
 */
var Metrics = (function($) {
  var metrics = [];

  // TODO: when we start getting a lot of these, it might be more efficient to pre-process them into
  // a map or something instead of looping.
  function getMetric(snapshot, name) {
    for (var i in snapshot.metric) {
      var metric = snapshot.metric[i];
      if (metric.name == name) {
        return metric;
      }
    }

    return null;
  }

  var TYPES = {
    "timer": function(data) {

      var dataTable = new google.visualization.DataTable();
      dataTable.addColumn("datetime", "Date");
      dataTable.addColumn("number", this.name);

      var rows = [];
      for (var i in data) {
        var snapshot = data[i];
        var metric = getMetric(snapshot, this.name);
        if (metric != null) {
          rows.push([
            new Date(parseInt(snapshot.time)),
            metric.timer.meter.m5Rate
          ]);
        }
      }
      dataTable.addRows(rows);
      var dataView = new google.visualization.DataView(dataTable);

      console.log(JSON.stringify(rows));

      var width = this.div.width();
      var height = this.div.height();
      var options = {
        "chartArea": {left: 50, top: 10, width: width - 200, height: height - 80},
        "backgroundColor": {fill: "transparent"}
      };

      var chart = new google.visualization.LineChart(document.getElementById(this.div.prop("id")));
      chart.draw(dataView, options);
    }
  };

  // Find all the graphs on the page and initialize their properties.
  function initGraphs() {
    $(".metrics-graph").each(function() {
      var div = $(this);
      metrics.push({
        "div": div,
        "name": div.data("name"),
        "type": div.data("type"),
        "refresh": TYPES[div.data("type")]
      });

      div.css("height", div.width() * 0.5);
    });
  }

  function refresh() {
    $.ajax({
      "method": "GET",
      "url": "/realms/" + window.realm + "/admin/metrics",
      "dataType": "json",
      "success": function(data) {
        for (var i = 0; i < metrics.length; i++) {
          metrics[i].refresh(data.snapshot);
        }
      },
      "error": function() {
        console.log("Couldn't fetch metrics.");
        // TODO: handle.
      }
    });
  }

  return {
    init: function() {
      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(refresh);
      initGraphs();
    }
  };
})(jQuery);

$(function() { Metrics.init(); });
