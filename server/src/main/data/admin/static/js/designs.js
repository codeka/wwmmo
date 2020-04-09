var Designs = (function() {
  var designs = null;
  $.ajax({
    url: "/admin/ajax/designs",
    success: function(data) {
      designs = data.designs;
      refreshDesigns();
    },
  });

  function getDesign(designType) {
    for (var i in designs) {
      if (designs[i].type == designType) {
        return designs[i];
      }
    }
    return null;
  }

  function refreshDesigns() {
    $("div.design-details").each(function(_, div) {
      var design = getDesign($(this).data("design_type"));
      if (design == null) {
        return;
      }

      $("img", this).prop("src", "/admin/img/sprites/" + design.image_url);
      $("span", this).html(design.display_name);
    });
/*
    $("span.design-label").each(function(_, span) {
      var designName = $(span).data("design");
      span.innerHTML = Designs.get(designName).display_name;
    });
*/
  }

  $(function() {
    refreshDesigns();
  });

  return {
    get: function(type) {
      return getDesign(type);
    }
  };
})();
