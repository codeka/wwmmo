$(function() {

  $("#send input").on({
    "keypress": function(evnt) {
      if (evnt.charCode == 13) {
        var msg = $("#send input").val();
        $("#send input").val("");

        $.ajax({
          url: "/admin/ajax/chat",
          data: data,
          success: function(data) {
            $("#result").html($("#create-empire-tmpl").applyTemplate(data));
          }
        });
      }
    }
  });
  $("#send input").focus();
});
