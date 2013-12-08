/*
 Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.

 For licensing, see LICENSE.md or http://ckeditor.com/license

*/
CKEDITOR.plugins.add("print",{lang:"en",icons:"print,",hidpi:!0,init:function(a){a.elementMode!=CKEDITOR.ELEMENT_MODE_INLINE&&(a.addCommand("print",CKEDITOR.plugins.print),a.ui.addButton&&a.ui.addButton("Print",{label:a.lang.print.toolbar,command:"print",toolbar:"document,50"}))}});CKEDITOR.plugins.print={exec:function(a){CKEDITOR.env.opera||(CKEDITOR.env.gecko?a.window.$.print():a.document.$.execCommand("Print"))},canUndo:!1,readOnly:1,modes:{wysiwyg:!CKEDITOR.env.opera}};