//
// Javascript tab library. By Dean Harding.
//
// Takes HTML like so:
//
// <ul class="tabs">
//  <li><a href="" data-tab="tab1">Tab 1</a>
//  <li><a href="" data-tab="tab2">Tab 2</a>
// </ul>
// <div id="tab1">Tab Content Here</div>
// <div id="tab2">Tab Content Here</div>
//
// And makes the tabs clickable and stuff.
//

const tabs = (function() {
 function refreshVisibleTabs(tabs) {
    $("a", tabs).each(function(i, tab) {
      var tabId = $(tab).data("tab");
      if (tabId) {
        var tabContent = $("#" + tabId);
        if ($(tab).hasClass("selected")) {
          tabContent.show();
          tabContent.trigger("tab:show");
        } else {
          tabContent.hide();
          tabContent.trigger("tab:hide");
        }
      }
    });
  }

  $(function() {
    refreshVisibleTabs($(".tabs"));

    $("body").on("click", ".tabs a", function() {
      $(this).parents(".tabs").find("a").removeClass("selected");
      $(this).addClass("selected");
      refreshVisibleTabs($(this).parents(".tabs"));
    });
  });

  return {
    refresh: function() {
      refreshVisibleTabs($(".tabs"));
    }
  }
})();
