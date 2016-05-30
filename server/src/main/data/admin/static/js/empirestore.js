
var empireStore = (function() {
  var storedEmpires = window.localStorage.getItem("empires");
  if (!storedEmpires) {
    storedEmpires = {};
  } else {
    storedEmpires = JSON.parse(storedEmpires);
  }
  var empires = {};

  function defaultCallback(empire) {
    $("span[data-empireid]").each(function(i, elem) {
      var $this = $(this);
      if ($this.data("empireid") == empire.id) {
        $this.html(empire.display_name);
      }
    });
  }

  var callbacks = {};

  return {
    getEmpire: function(empireId, callback) {
      if (typeof callback === "undefined") {
        callback = defaultCallback;
      }

      // If we have a cached empire already, just return that and we're done.
      if (typeof empires[empireId] != "undefined") {
        setTimeout(function() { callback(empires[empireId]); }, 0);
        return;
      }

      // If we have one stored, we can return that for now, but we'll want to re-fetch from the
      // server anyway, to ensure we have the freshest.
      if (typeof storedEmpires[empireId] != "undefined") {
        setTimeout(function() { callback(empires[empireId]); }, 0);
      }

      // If we already have a callback for this empireId, it means we're already fetching it, so
      // just add to the list for when we get the fresh data.
      if (typeof callbacks[empireId] != "undefined") {
        setTimeout(function() { callback(empires[empireId]); }, 0);
        return;
      }

      callbacks[empireId] = [callback];
      $.ajax({
        url: "/admin/ajax/empire",
        data: {
          id: empireId
        },
        success: function(data) {
          empires[data.id] = data;
          window.localStorage.setItem("empires", JSON.stringify(empires));
          for (var i = 0; i < callbacks[empireId].length; i++) {
            callbacks[empireId][i](data);
          }
        }
      });
    }
  }
})();
