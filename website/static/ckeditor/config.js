/**
 * Copyright (c) 2003-2016, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

CKEDITOR.stylesSet.add('code_styles', [
    { name: 'Styles', element: 'p', attributes: { 'class': '' } },
    { name: 'Java', element: 'pre', attributes: { 'class': 'brush: java;' } },
    { name: 'JavaScript', element: 'pre', attributes: { 'class': 'brush: js' } },
    { name: 'C++', element: 'pre', attributes: { 'class': 'brush: cpp' } },
    { name: 'C#', element: 'pre', attributes: { 'class': 'brush: csharp' } },
    { name: 'CSS', element: 'pre', attributes: { 'class': 'brush: css' } },
    { name: 'PHP', element: 'pre', attributes: { 'class': 'brush: php' } },
    { name: 'Plain Text', element: 'pre', attributes: { 'class': 'brush: text' } },
    { name: 'Python', element: 'pre', attributes: { 'class': 'brush: python' } },
    { name: 'SQL', element: 'pre', attributes: { 'class': 'brush: sql' } },
    { name: 'XML/HTML', element: 'pre', attributes: { 'class': 'brush: xml' } }
]);

CKEDITOR.editorConfig = function( config ) {

  config.toolbar = [
    { name: 'document', items: [ 'Source' ] },
    { name: 'basicstyles', items: [ 'Bold', 'Italic', '-', 'RemoveFormat' ] },
    { name: 'paragraph', items: [ 'NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote' ] },
    { name: 'links', items: [ 'Link', 'Unlink' ] },
    { name: 'insert', items: [ 'Image', 'Table', 'HorizontalRule', 'SpecialChar' ] },
    { name: 'styles', items: [ 'Styles', 'Format' ] },
    { name: 'tools', items: [ 'Maximize' ] },
  ];

  // Considering that the basic setup doesn't provide pasting cleanup features,
  // it's recommended to force everything to be plain text.
  config.forcePasteAsPlainText = true;

  config.stylesSet = 'code_styles';
};

