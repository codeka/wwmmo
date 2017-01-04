
function findEmptySector() {
  $.ajax({
    url: "/admin/ajax/sectors",
    data: {
      "action": "find-empty",
    },
    success: function(data) {
      $("#result").html(JSON.stringify(data));
    }
  });
}
