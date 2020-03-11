#!/usr/bin/env python
# encoding: utf8

# Babel adds python-format to every message that looks like a Python format
# string; it does so implicitly, and there is no  way to turn it off. It
# even does so when reading a catalog from disk, which gives us additional
# trouble when we try to update existing catalogs.
#
# The proper way to solve this is to patch Babel (TODO), but in the
# meantime, it's best to disable the behavior globally by monkeypatching
# the regex used to something that will never match.
#
# It's certainly a better solution than going through each loaded-from-disk
# catalog and removing all python-format flags, particularly since that
# approach would also remove possibly remove legitimate such flags. Granted,
# this isn't likely to happen a lot, but it's the cleaner approach.


from babel.messages import catalog
import re
catalog.PYTHON_FORMAT = re.compile('(?!)')

__version__ = (1, 5, 0)


def get_version():
    return ".".join(map(str, __version__)) + "_moz"
