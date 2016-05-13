
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
    }
  }

  function showStar(star) {
    var html = $("#star-details-tmpl").applyTemplate(star);
    $("#star-details").html(html);
  }

  $("#xy button").on("click", function() {
    $.ajax({
      method: "POST",
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

/*
{"id":925,
 "sector_x":0,
 "sector_y":0,
 "name":"Alaros",
 "classification":"WHITE",
 "size":19,
 "offset_x":545,
 "offset_y":632,
 "planets":[
   {"index":1,"planet_type":"DESERT","population_congeniality":308,"farming_congeniality":7,"mining_congeniality":36,"energy_congeniality":11},
   {"index":2,"planet_type":"INFERNO","population_congeniality":207,"farming_congeniality":9,"mining_congeniality":44,"energy_congeniality":122},
   {"index":3,"planet_type":"TOXIC","population_congeniality":183,"farming_congeniality":14,"mining_congeniality":9,"energy_congeniality":35},
   {"index":4,"planet_type":"DESERT","population_congeniality":272,"farming_congeniality":36,"mining_congeniality":21,"energy_congeniality":14},
   {"index":5,"planet_type":"GASGIANT","population_congeniality":209,"farming_congeniality":14,"mining_congeniality":31,"energy_congeniality":18},
   {"index":6,"planet_type":"SWAMP","population_congeniality":330,"farming_congeniality":57,"mining_congeniality":18,"energy_congeniality":35},
   {"index":7,"planet_type":"WATER","population_congeniality":501,"farming_congeniality":61,"mining_congeniality":10,"energy_congeniality":42}
  ]}
*/