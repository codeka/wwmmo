var Designs = (function() {
  var designs = null;
  $.ajax({
    url: "/admin/ajax/designs",
    success: function(data) {
      designs = data.designs;
      refreshDesigns();
    },
  });

  function refreshDesigns() {
    $("img.fleet-icon").each(function(_, img) {
      var designName = $(img).data("design");
      $(img).prop("src", "/admin/img/sprites/" + Designs.get(designName).image_url);
    });

    $("span.fleet-label").each(function(_, span) {
      var designName = $(span).data("design");
      span.innerHTML = Designs.get(designName).display_name;
    });
  }

  return {
    get: function(type) {
      for (var i in designs) {
        if (designs[i].type == type) {
          return designs[i];
        }
      }
      return null;
    }
  };
})();
