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
      dataTable.addColumn("number", "QPS");
      dataTable.addColumn("number", "Time 75%");
      dataTable.addColumn("number", "Time 98%");

      var rows = [];
      for (var i in data) {
        var snapshot = data[i];
        var metric = getMetric(snapshot, this.name);
        if (metric != null) {
          rows.push([
            new Date(parseInt(snapshot.time)),
            metric.timer.meter.m5_rate,
            metric.timer.histogram.p75 / 1000000.0,
            metric.timer.histogram.p98 / 1000000.0
          ]);
        }
      }
      dataTable.addRows(rows);
      var dataView = new google.visualization.DataView(dataTable);

      var width = this.div.width();
      var height = this.div.height();
      var options = {
        "chart": {
          "title": this.name
        },
        "chartArea": {"left": 50, "top": 10, "width": width - 120, "height": height - 80},
        "series": {
          0: {"targetAxisIndex": 0},
          1: {"targetAxisIndex": 1},
          2: {"targetAxisIndex": 1}
        },
        "vAxes": {
          0: {"title": "QPS", "minValue": 0},
          1: {"title": "Time (ms)", "minValue": 0}
        },
     //  "curveType": "function",
        "backgroundColor": {fill: "transparent"},
        "legend": { "position": "bottom" }
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
