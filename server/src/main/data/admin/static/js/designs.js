var Designs = (function() {
  var designs = null;
  $.ajax({
    url: "/admin/ajax/designs",
    success: function(data) {
      designs = data.designs;
    },
  });

  return {
    get: function(id) {
      console.log(JSON.stringify(designs));
      for (var i in designs) {
        if (designs[i].id == id) {
          return designs[i];
        }
      }
      return null;
    }
  };
})();
