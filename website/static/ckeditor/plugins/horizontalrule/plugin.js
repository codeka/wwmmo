/*
 Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.

 For licensing, see LICENSE.md or http://ckeditor.com/license

*/
(function(){var b={canUndo:!1,exec:function(a){var b=a.document.createElement("hr");a.insertElement(b)},allowedContent:"hr",requiredContent:"hr"};CKEDITOR.plugins.add("horizontalrule",{lang:"en",icons:"horizontalrule",hidpi:!0,init:function(a){a.blockless||(a.addCommand("horizontalrule",b),a.ui.addButton&&a.ui.addButton("HorizontalRule",{label:a.lang.horizontalrule.toolbar,command:"horizontalrule",toolbar:"insert,40"}))}})})();