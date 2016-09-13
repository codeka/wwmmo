
$(function() {
  var starHtmlTemplate = [
    "<h1>{%= name %}</h1>",
  ].join("\n");

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
    var html = $("#star-details-tmpl").applyTemplate(star);
    $("#star-details").html(html);
    fix_times();
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
});
