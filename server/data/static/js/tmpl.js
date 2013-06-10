// Simple JavaScript Templating
// John Resig - http://ejohn.org/ - MIT Licensed
// Modified by Dean Harding, 2012
//  - made it a jQuery plugin: var html = $("#my-template").applyTemplate(data);
//    calling applyTemplate() more than one re-uses the compiled template
//    automatically.

(function($){
  $.fn.applyTemplate = function(data) {
    if (this.data("applyTemplate-cache") == null) {
      var tmpl = this.html();

      this.data("applyTemplate-cache", new Function("obj",
        "var p=[],print=function(){p.push.apply(p,arguments);};" +

        // introduce the data as local variables using with(){}
        "with(obj){p.push('" +

        // convert the template into pure JavaScript
        tmpl.replace(/[\r\t\n]/g, " ")
            .replace(/<%/g, "\t")
            .replace(/((^|%>)[^\t]*)'/g, "$1\r")
            .replace(/\t=(.*?)%>/g, "',$1,'")
            .replace(/\t/g, "');")
            .replace(/%>/g, "p.push('")
            .replace(/\r/g, "\\'")
          + "');}return p.join('');")
         );
    }
    fn = this.data("applyTemplate-cache");

    return fn(data);
  };
})(jQuery);
