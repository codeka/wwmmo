
function findEmptySector() {
  $.ajax({
    url: "/admin/ajax/sectors",
    data: {
      "action": "find-empty",
    },
    success: function(data) {
      $("#result").html(JSON.stringify(data, null, "  "));
    }
  });
}

function createEmpire(name, x, y) {
  if (name === undefined) {
    name = $("#empire-name").val();
  }

  var data = {
    "action": "create-empire",
    "name": name
  };
  if (x === undefined && $("#empire-sector-x").val() !== "") {
    x = parseInt($("#empire-sector-x").val());
  }
  if (y === undefined && $("#empire-sector-y").val() !== "") {
    y = parseInt($("#empire-sector-y").val());
  }
  if (x !== undefined && x !== undefined) {
    data["x"] = x;
    data["y"] = y;
  }

  $.ajax({
    url: "/admin/ajax/sectors",
    data: data,
    success: function(data) {
      $("#result").html($("#create-empire-tmpl").applyTemplate(data));
    }
  });
}
