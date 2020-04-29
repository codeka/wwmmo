
function setupTab(tabId) {
  $("#" + tabId).on("tab:show", function() {
    var content = $("#" + tabId);
    if (content.hasClass("loaded")) {
      return;
    }
    content.addClass("loaded");

    $.ajax({
        url: "/admin/empires/" + empire.id + "?tab=" + tabId,
        success: function(data) {
          content.html(data);
        }, error: function(xhr, status, err) {
          content.html("ERROR: " + status + " : " + err)
        }
    });
  });
}

$(function() {
  setupTab("stars");
  setupTab("devices");
  setupTab("sit-reports");
});
