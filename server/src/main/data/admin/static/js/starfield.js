
$(function() {
  var starHtmlTemplate = [
    "<h1>{%= name %}</h1>",
  ].join("\n");

  var currStar = null;

  function renderSector(sector) {
    var container = $("#starfield");
    container.empty();

    for (var index in sector.stars) {
      var star = sector.stars[index];
      var div = $("<div/>");
      div.addClass("starfield-star");
      div.addClass("star-" + star.classification.toLowerCase());
      div.css("left", parseInt(star.offset_x * 0.5) + "px");
      div.css("top", parseInt(star.offset_y * 0.5) + "px");
      div.prop("title", star.name);
      div.data("star", star);
      div.on("click", function() { showStar($(this).data("star")); });
      container.append(div);

      var label = $("<div/>");
      label.addClass("starfield-label");
      label.addClass("noselect");
      label.css("left", parseInt(star.offset_x * 0.5) + "px");
      label.css("top", parseInt(star.offset_y * 0.5) + "px");
      label.text(star.name);
      label.data("star", star);
      label.on("click", function() { showStar($(this).data("star")); });
      container.append(label);

      var empires = [];
      for (var i in star.planets) {
        var planet = star.planets[i];
        if (planet.colony != null && planet.colony.empire_id != null) {
          if (empires.indexOf(planet.colony.empire_id) === -1) {
            empires.push(planet.colony.empire_id);
          }
        }
      }
      for (var i in star.fleets) {
        var fleet = star.fleets[i];
        if (fleet.empire_id != null) {
          if (empires.indexOf(fleet.empire_id) === -1) {
            empires.push(fleet.empire_id);
          }
        }
      }
      for (var i in empires) {
        label = $("<div class=\"starfield-empire\"><img /><span>...</span></div>");
        $("span", label).attr("data-empireid", empires[i]).attr("data-nolink", "1");
        var angle = (Math.PI / 4.0) * (i + 1);
        var x = 0.0;
        var y = -20.0;
        var nx = (x * Math.cos(angle) - y * Math.sin(angle));
        var ny = (y * Math.cos(angle) + x * Math.sin(angle));
        label.css("left", parseInt(star.offset_x * 0.5 + nx) + "px");
        label.css("top", parseInt(star.offset_y * 0.5  + ny) + "px");
        $("img", label)
            .attr("src", "/render/empire/" + empires[i] + "/16x16/mdpi.png")
            .css("vertical-align", "bottom")
            .css("margin-right", "2px");
        container.append(label);
        empireStore.getEmpire(empires[i]);
      }
    }
  }

  function showStar(star) {
    currStar = star;
    var html = $("#star-details-tmpl").applyTemplate(star);
    $("#star-details").html(html);
    fixTimes();
  }

  window.modify = function(id) {
    if (currStar == null) {
      return;
    }

    $("#modify-popup").show();

    var planetIndexSelect = $("#modify-popup select[name=planet_index]");
    var colonySelect = $("#modify-popup select[name=colony_id]");
    var fleetSelect = $("#modify-popup select[name=fleet_id]");
    planetIndexSelect.empty();
    colonySelect.empty();
    fleetSelect.empty();

    planetIndexSelect.append($("<option>None</option>"));
    colonySelect.append($("<option>None</option>"));
    fleetSelect.append($("<option>None</option>"));
    var empires = [];

    for (var i = 0; i < currStar.planets.length; i++) {
      var planet = currStar.planets[i];
      var opt = $("<option/>");
      opt.attr("value", i);
      opt.html((i + 1) + ": "
          + toTitleCase(planet.planet_type)
          + " &nbsp; \uD83D\uDC6A " + planet.population_congeniality
          + " &nbsp; \u2618 " + planet.farming_congeniality
          + " &nbsp; \u26cf " + planet.mining_congeniality
          + " &nbsp; \u26a1 " + planet.energy_congeniality);
      planetIndexSelect.append(opt);

      if (planet.colony != null) {
        opt = $("<option/>");
        opt.attr("value", planet.colony.id);
        if (empires.indexOf(planet.colony.empire_id) < 0) {
          empires.push(planet.colony.empire_id);
        }
        colonySelect.append(opt);
      }
    }

    for (var i = 0; i < currStar.fleets.length; i++) {
      var fleet = currStar.fleets[i];
      var opt = $("<option/>");
      opt.attr("value", fleet.id);
      if (empires.indexOf(fleet.empire_id) < 0) {
        empires.push(fleet.empire_id);
      }
      fleetSelect.append(opt);
    }

    empires.forEach(function(empireId) {
      empireStore.getEmpire(empireId, function(empire) {
        $("#modify-popup select[name=colony_id]").children().each(function(index, elem) {
          var colonyId = $(elem).attr("value");
          if (!colonyId) {
            return;
          }
          for (var planetIndex = 0; planetIndex < currStar.planets.length; planetIndex++) {
            var planet = currStar.planets[planetIndex];
            if (planet.colony && planet.colony.id == colonyId) {
              $(elem).html(
                  (planetIndex + 1) + ": "
                  + toTitleCase(planet.planet_type)
                  + " (" + empire.display_name + ")");
            }
          }
        });
        $("#modify-popup select[name=fleet_id]").children().each(function(index, elem) {
          var fleetId = $(elem).attr("value");
          if (!fleetId) {
            return;
          }
          for (var fleetIndex = 0; fleetIndex < currStar.fleets.length; fleetIndex++) {
            var fleet = currStar.fleets[fleetIndex];
            var design = Designs.get(fleet.design_type);
            if (fleet.id == fleetId) {
              $(elem).html(
                  (fleetIndex + 1) + ": "
                  + toTitleCase(design.display_name)
                  + " x" + fleet.num_ships
                  + " (" + empire.display_name + ")");
            }
          }
        });
      });
    });

    $("#modify-cancel").on("click", function() {
      $("#modify-popup").hide();
    });
    $("#modify-ok").on("click", function() {
      $("#modify-popup").hide();

      // TODO: modify
    });
  }

  window.simulate = function(id) {
    $.ajax({
      url: "/admin/ajax/simulate",
      data: {
        "id": id
      },
      method: "POST",
      success: function(data) {
        var html = $("#simulate-result-tmpl").applyTemplate(data);
        $("#simulate-result").html(html);
      }
    })
  }

  $("#xy button").on("click", function() {
    $.ajax({
      url: "/admin/ajax/starfield",
      data: {
        "action": "xy",
        "x": $("#xy input[name=x]").val(),
        "y": $("#xy input[name=y]").val()
      },
      success: function(data) {
        renderSector(data);
      }
    });
  });

  $("#starfield-container a").on("click", function() {
    var dx = 0;
    var dy = 0;
    if (this.id == "starfield-up-btn") {
      dy = -1;
    } else if (this.id == "starfield-down-btn") {
      dy = 1;
    } else if (this.id == "starfield-left-btn") {
      dx = -1;
    } else if (this.id == "starfield-right-btn") {
      dx = 1;
    }
    $("#xy input[name=x]").val(parseInt($("#xy input[name=x]").val()) + dx);
    $("#xy input[name=y]").val(parseInt($("#xy input[name=y]").val()) + dy);
    $("#xy button").click();
  });
});
