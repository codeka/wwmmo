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

    var header = $("<div class=\"header\" />");
    if (msg.empire_id) {
      var img = $("<img />")
         .attr("data-empireid", msg.empire_id)
         .attr("width", "16")
         .attr("height", "16");
      var span = $("<span />")
         .attr("data-empireid", msg.empire_id);
      header.append($("<div class=\"empire\">").append(img).append(span));
    }
    header.append($("<time timestamp=\"" + msg.date_posted + "\"></time>"));
    div.append(header);

    var content = $("<div class=\"content\">").html(msg.message);
    if (!msg.empire_id) {
      content.addClass("server");
    }
    div.append(content);

    $("#messages").append(div);
    if (lastMsgTime < msg.date_posted) {
      lastMsgTime = msg.date_posted;
    }

    time.refreshAll();
    if (msg.empire_id != null) {
      empireStore.getEmpire(msg.empire_id);
    }
    div.get(0).scrollIntoView();
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
