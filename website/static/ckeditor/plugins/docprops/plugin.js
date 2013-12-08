/*
 Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.

 For licensing, see LICENSE.md or http://ckeditor.com/license

*/
CKEDITOR.plugins.add("docprops",{requires:"wysiwygarea,dialog",lang:"en",icons:"docprops,docprops-rtl",hidpi:!0,init:function(a){var b=new CKEDITOR.dialogCommand("docProps");b.modes={wysiwyg:a.config.fullPage};b.allowedContent={body:{styles:"*",attributes:"dir"},html:{attributes:"lang,xml:lang"}};b.requiredContent="body";a.addCommand("docProps",b);CKEDITOR.dialog.add("docProps",this.path+"dialogs/docprops.js");a.ui.addButton&&a.ui.addButton("DocProps",{label:a.lang.docprops.label,command:"docProps",
toolbar:"document,30"})}});