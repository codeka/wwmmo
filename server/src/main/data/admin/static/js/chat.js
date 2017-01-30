$(function() {

  $("#send input").on({
    "keypress": function(evnt) {
      if (evnt.charCode == 13) {
        var data = {
          "action": "send",
          "msg": $("#send input").val(),
        };

        $.ajax({
          url: "/admin/ajax/chat",
          method: "POST",
          data: data,
          success: function(data) {
            $("#send input").val("");
          }
        });
      }
    }
  });
  $("#send input").focus();
});
