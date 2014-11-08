//
// Library for displaying "popup" divs on a page. Use like so:
//
// <div id="mypopup" class="popup"> ... </div>
//
// $("#mypopup").popup();
// $("#mypopup").popup("close");
//

(function($) {
  var scrim = null;

  $.fn.popup = function(action) {
    if (scrim == null) {
      scrim = $("#popup-scrim");
    }

    if (typeof action == "undefined" || action == "open") {
      this.detach().appendTo(scrim);
      scrim.show();
      this.show();

      var $popup = this;
      this.find(".popup-buttons .close").click(function() {
        $popup.popup("close");
      });
    } else if (action == "close") {
      scrim.hide();
      this.hide();
    }
  }
})(jQuery);