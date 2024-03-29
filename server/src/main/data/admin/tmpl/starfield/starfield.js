
$(function() {
  let currStar = null;
  let currSectorX = 0;
  let currSectorY = 0;

  // Describe the sector state, keep in sync with SectorsStore.SectorState.
  function describeState(state) {
    switch(state) {
    case 0:
      return "New";
    case 1:
      return "Empty";
    case 2:
      return "Non-empty";
    case 3:
      return "Abandoned";
    default:
      return "Unknown: " + state;
    }
  }

  function renderSector(sector) {
    const container = $("#starfield");
    container.empty();

    let div = $("<div/>");
    div.addClass("sector-state");
    div.text(describeState(sector.state));
    container.append(div);

    for (let index in sector.stars) {
      const star = sector.stars[index];
      div = $("<div/>");
      div.addClass("star-small");
      div.addClass("star-" + star.classification.toLowerCase());
      div.css("left", parseInt(star.offset_x * 0.5) + "px");
      div.css("top", parseInt(star.offset_y * 0.5) + "px");
      div.prop("title", star.name);
      div.data("star", star);
      div.on("click", function() { showStar($(this).data("star")); });
      container.append(div);

      let label = $("<div/>");
      label.addClass("starfield-label");
      label.addClass("noselect");
      label.css("left", parseInt(star.offset_x * 0.5) + "px");
      label.css("top", parseInt(star.offset_y * 0.5) + "px");
      label.text(star.name);
      label.data("star", star);
      label.on("click", function() { showStar($(this).data("star")); });
      container.append(label);

      const empires = [];
      for (let i in star.planets) {
        const planet = star.planets[i];
        if (planet.colony != null && planet.colony.empire_id !== 0) {
          if (empires.indexOf(planet.colony.empire_id) === -1) {
            empires.push(planet.colony.empire_id);
          }
        }
      }
      for (let i in star.fleets) {
        const fleet = star.fleets[i];
        if (fleet.empire_id !== 0) {
          if (empires.indexOf(fleet.empire_id) === -1) {
            empires.push(fleet.empire_id);
          }
        }
      }
      for (let i in empires) {
        label = $("<div class=\"starfield-empire\"><img /><span>...</span></div>");
        $("span", label).attr("data-empireid", empires[i]).attr("data-nolink", "1");
        var angle = (Math.PI / 4.0) * (i + 1);
        var x = 0.0;
        var y = -20.0;
        var nx = (x * Math.cos(angle) - y * Math.sin(angle));
        var ny = (y * Math.cos(angle) + x * Math.sin(angle));
        label.css("left", (star.offset_x * 0.5 + nx).toFixed() + "px");
        label.css("top", (star.offset_y * 0.5  + ny).toFixed() + "px");
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
    if (star) {
      const html = $("#star-details-tmpl").applyTemplate(star);
      $("#star-details").html(html);
      time.refreshAll();
    } else {
      $("#star-details").empty();
    }
    updateUrl();
  }

  // Called to refresh which fields are visible, based on the modification type you've selected.
  function refreshVisibleFields() {
    const VISIBLE_FIELDS = {
      "COLONIZE": ["empire_id", "planet_index"],
      "ADJUST_FOCUS": [/*"empire_id", "colony_id", "focus"*/],
      "CREATE_FLEET": ["empire_id", "design_type", "count", "full_fuel"],
      "ADD_BUILD_REQUEST": ["empire_id", "colony_id", "design_type", "count"],
      "CREATE_BUILDING": ["empire_id", "colony_id", "design_type"],
      "SPLIT_FLEET": ["empire_id", "fleet_id", "count"],
      "MERGE_FLEET": ["empire_id", "fleet_id", "additional_fleet_ids"],
      "MOVE_FLEET": [/*"empire_id", "fleet_id", "star_id"*/],
      "DELETE_BUILD_REQUEST": ["empire_id", "build_request_id"]
    };

    const type = $("#modify-popup select[name=type]").val()
    const $parent = $("#modify-popup dl");

    // The <dl> will be a bunch of <dt><dd> pairs. The <dd> will have a child that has a name
    // attribute which is the name of the field we'll want to show/hide. If we want to hide the
    // field, we need to hide both the <dt> and <dd>
    $("dd", $parent).each(function (index, dd) {
      let $dd = $(dd);
      let $dt = $dd.prev();

      $dd.find("[name]").each(function (_, input) {
        const name = $(input).attr("name");
        if (name === "type") {
          return;
        }

        if (VISIBLE_FIELDS[type].indexOf(name) >= 0) {
          $dd.show();
          $dt.show();
        } else {
          $dd.hide();
          $dt.hide();
        }
      });
    });
  }

  window.modify = function(id) {
    if (currStar == null) {
      return;
    }

    $("#modify-popup").show();

    const planetIndexSelect = $("#modify-popup select[name=planet_index]");
    const colonySelect = $("#modify-popup select[name=colony_id]");
    const fleetSelect = $("#modify-popup select[name=fleet_id]");
    const buildRequestsSelect = $("#modify-popup select[name=build_request_id]");
    planetIndexSelect.empty();
    colonySelect.empty();
    fleetSelect.empty();
    buildRequestsSelect.empty();

    planetIndexSelect.append($("<option>None</option>"));
    colonySelect.append($("<option>None</option>"));
    fleetSelect.append($("<option>None</option>"));
    buildRequestsSelect.append($("<option>None</option>"));
    let empires = [];

    for (let i = 0; i < currStar.planets.length; i++) {
      const planet = currStar.planets[i];
      let opt = $("<option/>");
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

    for (let i = 0; i < currStar.fleets.length; i++) {
      const fleet = currStar.fleets[i];
      opt = $("<option/>");
      opt.attr("value", fleet.id);
      if (empires.indexOf(fleet.empire_id) < 0) {
        empires.push(fleet.empire_id);
      }
      fleetSelect.append(opt);
    }

    empires.forEach(function(empireId) {
      empireStore.getEmpire(empireId, function(empire) {
        $("#modify-popup select[name=colony_id]").children().each(function(index, elem) {
          const colonyId = $(elem).attr("value");
          if (!colonyId) {
            return;
          }
          for (let planetIndex = 0; planetIndex < currStar.planets.length; planetIndex++) {
            const planet = currStar.planets[planetIndex];
            if (planet.colony && planet.colony.id === colonyId) {
              $(elem).html(
                  (planetIndex + 1) + ": "
                  + toTitleCase(planet.planet_type)
                  + " (" + empire.display_name + ")");
            }
          }
        });
        $("#modify-popup select[name=fleet_id]").children().each(function(index, elem) {
          const fleetId = $(elem).attr("value");
          if (!fleetId) {
            return;
          }
          for (let fleetIndex = 0; fleetIndex < currStar.fleets.length; fleetIndex++) {
            const fleet = currStar.fleets[fleetIndex];
            const design = Designs.get(fleet.design_type);
            if (fleet.id === fleetId) {
              $(elem).html(
                  (fleetIndex + 1) + ": "
                  + toTitleCase(design.display_name)
                  + " x" + fleet.num_ships
                  + " (" + empire.display_name + ")");
            }
          }
        });

        for (let i = 0; i < currStar.planets.length; i++) {
          const planet = currStar.planets[i];
          if (planet.colony == null) {
            continue;
          }
          if (planet.colony.empire_id !== empire.id) {
            continue;
          }
          for (let j = 0; j < planet.colony.build_requests.length; j++) {
            const buildRequest = planet.colony.build_requests[j];
            const html = empire.display_name + ": " + buildRequest.id + " " + buildRequest.design_type
                + " x " + buildRequest.count
                + " (progress=" + buildRequest.progress + " finish: " + time.formatTime(new Date(buildRequest.end_time)) + ")";
            const opt = $("<option/>");
            opt.attr("value", buildRequest.id);
            opt.html(html);
            buildRequestsSelect.append(opt);
          }
        }

      });
    });

    const empireSelect = $("#modify-popup select[name=empire_id]");
    empireSelect.empty();
    empireSelect.append($("<option>None</option>"));
    empireSelect.append($("<option value=\"0\">Native</option>"));
    empires = empireStore.getAllEmpires();
    for (let i in empires) {
      const empire = empires[i];
      const opt = $("<option/>");
      opt.attr("value", empire.id);
      opt.html(empire.display_name);
      empireSelect.append(opt);
    }

    $("#modify-cancel").on("click", function() {
      $("#modify-popup").hide();
    });
    $("#modify-ok").on("click", function() {
      const additionalFleetIds = [];
      const additionalFleetIdsStrings =
          $("#modify-popup input[name=additional_fleet_ids]").val().split(",");
      for (var i in additionalFleetIdsStrings) {
        if (additionalFleetIds[i]) {
          additionalFleetIds.push(parseInt(additionalFleetIdsStrings[i]));
        }
      }
      const json = {
        type: $("#modify-popup select[name=type]").val(),
        empire_id: parseInt($("#modify-popup select[name=empire_id]").val()),
        planet_index: parseInt($("#modify-popup select[name=planet_index]").val()),
        colony_id: parseInt($("#modify-popup select[name=colony_id]").val()),
        fleet_id: parseInt($("#modify-popup select[name=fleet_id]").val()),
        // TODO focus:
        design_type: $("#modify-popup select[name=design_type]").val(),
        count: parseInt($("#modify-popup input[name=count]").val()),
        // TODO star_id:
        // TODO fleet:
        additional_fleet_ids: additionalFleetIds,
        full_fuel: $("#modify-popup input[name=full_fuel]").prop("checked"),
        build_request_id: parseInt($("#modify-popup select[name=build_request_id]").val())
      };

      // TODO: disable modify/cancel buttons.
      $.ajax({
        url: "/admin/ajax/starfield",
        data: {
          "action": "modify",
          "id": id,
          "modify": JSON.stringify(json)
        },
        method: "POST",
        success: function(data) {
          const html = $("#simulate-result-tmpl").applyTemplate(data);
          $("#simulate-result").html(html);

          $("#modify-popup").hide();
        }
      });
    });
    $("#modify-popup select[name=type]").on("change", function() {
      refreshVisibleFields();
    });

    refreshVisibleFields();
  }

  window.simulate = function(id) {
    $.ajax({
      url: "/admin/ajax/starfield",
      data: {
        "action": "simulate",
        "id": id
      },
      method: "POST",
      success: function(data) {
        const html = $("#simulate-result-tmpl").applyTemplate(data);
        $("#simulate-result").html(html);
      }
    })
  }

  window.deleteStar = function(id) {
    if (confirm("Are you sure you want to delete this star? This cannot be undone!")) {
      $.ajax({
        url: "/admin/ajax/starfield",
        method: "POST",
        data: {
          "action": "delete",
          "id": id
        },
        success: function(data) {
        refreshSector();
        }
      });
    }
  }

  window.clearNatives = function(id) {
    $.ajax({
      url: "/admin/ajax/starfield",
      method: "POST",
      data: {
        "action": "clearNatives",
        "id": id
      },
      success: function(data) {
        refreshSector();
      }
    });
  }

  $("#xy button").on("click", function() {
    currSectorX = $("#xy input[name=x]").val();
    currSectorY = $("#xy input[name=y]").val();
    currStar = null;
    refreshSector();
    updateUrl();
  });

  $("#star button").on("click", function() {
    const starId = $("#star input[name=star_id]").val();
    $.ajax({
      url: "/admin/ajax/starfield",
      data: { "action": "search", "star_id": starId },
      success: function(star) {
        location.href = "/admin/starfield?sector=" + star.sector_x + "," + star.sector_y +
                        "&star=" + star.id;
      }
    });
  });

  $("#star input[type=number]").on("keypress", function (e) {
    if (e.which === 13) {
      $("#star button").click();
    }
  });

  function updateUrl() {
    $.QueryString["sector"] = currSectorX + "," + currSectorY;
    if (currStar == null) {
      delete $.QueryString["star"];
    } else {
      $.QueryString["star"] = currStar.id;
    }
    history.replaceState(null, "", "?" + $.param($.QueryString));
  }

  window.refreshSector = function(currStarId) {
    currStarId = currStarId ? currStarId : (currStar == null ? 0 : currStar.id);
    $.ajax({
      url: "/admin/ajax/starfield",
      data: {
        "action": "xy",
        "x": currSectorX,
        "y": currSectorY
      },
      success: function(data) {
        renderSector(data);
        let shown = false;
        for (let index in data.stars) {
          if (data.stars[index].id === currStarId) {
            showStar(data.stars[index]);
            shown = true;
          }
        }
        if (!shown) {
          showStar(null);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        console.error(textStatus + " " + errorThrown);
      }
    });
  }

  window.resetCurrentSector = function() {
    if (!confirm("Are you sure you want to reset sector " + currSectorX + ", " + currSectorY + "? "
        + "This is permanent and cannot be undone!")) {
      return;
    }

    $.ajax({
      url: "/admin/ajax/starfield",
      method: "POST",
      data: {
        "action": "resetSector",
        "x": currSectorX,
        "y": currSectorY
      },
      success: function(data) {
        refreshSector();
      },
    });

  }

  window.showRaw = function() {
    $("#simulate-result").html("<pre><code>" + JSON.stringify(currStar, null, 2) + "</code></pre>");
  }

  $("#starfield-container a").on("click", function() {
    let dx = 0;
    let dy = 0;
    if (this.id === "starfield-up-btn") {
      dy = -1;
    } else if (this.id === "starfield-down-btn") {
      dy = 1;
    } else if (this.id === "starfield-left-btn") {
      dx = -1;
    } else if (this.id === "starfield-right-btn") {
      dx = 1;
    }
    $("#xy input[name=x]").val(parseInt($("#xy input[name=x]").val()) + dx);
    $("#xy input[name=y]").val(parseInt($("#xy input[name=y]").val()) + dy);
    $("#xy button").click();
  });

  if ($.QueryString["sector"]) {
    var sector = $.QueryString["sector"].split(",");
    currSectorX = parseInt(sector[0]);
    currSectorY = parseInt(sector[1]);
    $("#xy input[name=x]").val(currSectorX);
    $("#xy input[name=y]").val(currSectorY);
    var currStarId = null;
    if ($.QueryString["star"]) {
      currStarId = parseInt($.QueryString["star"]);
    }
    refreshSector(currStarId);
  }
});
