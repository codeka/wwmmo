
var empireStore = (function() {
    var empire_names = window.localStorage.getItem("empires");
    if (!empire_names) {
      empire_names = {};
    } else {
      empire_names = JSON.parse(empire_names);
    }

    var is_sorted = false;
    var original_page_title = document.title;
    var num_unread = 0;

    var callbacks = {};

    return {
      getEmpire: function(empireKey, callback) {
        if (typeof empire_names[empireKey] != "undefined") {
          callback(empire_names[empireKey]);
        } else {
          if (typeof callbacks[empireKey] != "undefined") {
            callbacks[empireKey].push(callback);
            return;
          }

          callbacks[empireKey] = [callback];
          $.ajax({
            "url": "/realms/"+window.realm+"/empires/search?ids="+empireKey,
            "dataType": "json",
            "method": "GET",
            "success": function(data) {
              data = data.empires[0];
              empire_names[data.key] = data.display_name;
              window.localStorage.setItem("empires", JSON.stringify(empire_names));
              for (var i = 0; i < callbacks[empireKey].length; i++) {
                callbacks[i](data.display_name);
              }
            }
          });
        }
      }
    }
})();
