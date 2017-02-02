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

  var lastMsgTime = new Date().getTime() - (3600000 /* 1 hour */ * 24);

  function appendMessage(msg) {
    var dt = new Date(msg.date_posted);
    var div = $("<div class=\"msg\"/>");
    div.html(dt + " " + msg.message);

    $("#messages").append(div);
    if (lastMsgTime < msg.date_posted) {
      lastMsgTime = msg.date_posted;
    }
  }

  setInterval(function() {
    console.log("sending request");
    $.ajax({
      url: "/admin/ajax/chat",
      data: {
        action: "recv",
        lastMsgTime: lastMsgTime,
      },
      success: function(data) {
        for (var i = 0; i < data.messages.length; i++) {
          appendMessage(data.messages[i]);
        }
      }
    })
  }, 1000);
});
