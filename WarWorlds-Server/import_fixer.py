import os
import sys

# By default, we cannot include packages with the prefix 'google' in App Engine
# applications. However, we want to use protocol buffers, and they're pretty hard-
# wired to use the google. package prefix. So this'll let us use it regardless.
#
# See: http://groups.google.com/group/google-appengine/msg/12e6c7d9bcda7a46
#
# Usage:
# import import_fixer
# import_fixer.FixImports('google', 'protobuf')


def FixImports(base_package, *packages):
  topdir = os.path.dirname(__file__)

  def ImportPackage(full_package):
    """Import a fully qualified package."""
    __import__(full_package, globals(), locals())

    # Check if the override path already exists for the module; if it does,
    # that means we've already fixed imports.
    original_module = sys.modules[full_package]
    lib_path = os.path.join(topdir, full_package.replace('.', '/'))

    if lib_path not in original_module.__path__:
      # Insert after runtime path, but before anything else
      original_module.__path__.insert(1, lib_path)

  ImportPackage(base_package)

  for package in packages:
    # For each package, we need to import all of its parent packages.
    dirs = package.split('.')
    full_package = base_package

    for my_dir in dirs:
      full_package = '%s.%s' % (full_package, my_dir)
      ImportPackage(full_package)
