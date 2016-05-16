
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
