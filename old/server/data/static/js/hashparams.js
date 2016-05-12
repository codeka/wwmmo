
window.hashparams = (function($) {

  function parseHash() {
    var e,
        a = /\+/g,  // regex for replacing addition symbol with a space
        r = /([^&;=]+)=?([^&;]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.hash.substring(1);

    var params = {};
    while (e = r.exec(q)) {
      params[d(e[1])] = d(e[2]);
    }
    return params;
  }

  return {
    register: function(fn) {
      $(window).on("hashchange", function() {
        fn(parseHash());
      });
      if (window.location.hash.substring(1) != "") {
        fn(parseHash());
      }
    },

    get: function() {
      return parseHash();
    }
  }
})(jQuery);
