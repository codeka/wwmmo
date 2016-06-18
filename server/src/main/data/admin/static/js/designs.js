var Designs = (function() {
  var designs = null;
  $.ajax({
    url: "/admin/ajax/designs",
    success: function(data) {
      designs = data.designs;
    },
  });

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
