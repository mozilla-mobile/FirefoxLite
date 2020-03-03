# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# -*- coding: utf-8 -*-
import os
import io
from lxml import etree

customized_folder = '../components/customizedResources/src/main/res'
customized_strings_file = 'strings_customized.xml'

locale_folder = 'src/main/res'
locale_strings_file = 'strings.xml'

print(os.getcwd())

for dirpath, _, _ in os.walk(customized_folder):
    dirname = dirpath.split(os.path.sep)[-1]
    if dirname.startswith('values-') and os.path.exists(os.path.join(dirpath, customized_strings_file)):
        print("LOCALE: %s" % dirname)

        # customizedResources
        with io.open(os.path.join(dirpath, customized_strings_file), 'rb') as fp:
            customized = etree.parse(fp)
        customized_root = customized.getroot()

        # original locale string.xml
        with io.open(os.path.join(locale_folder, dirname, locale_strings_file), 'rb') as fp:
            locale = etree.parse(fp)
        locale_root = locale.getroot()
        localized_strings_name = [x.attrib['name'] for x in locale_root.iter(tag='string')]

        for s in customized.iter(tag='string'):
            if s.attrib['name'] in localized_strings_name:
                print("Remove duplicated string: %s" % s.attrib['name'])
                customized_root.remove(s)

        with open(os.path.join(dirpath, customized_strings_file), 'wb') as f:
            f.write(etree.tostring(customized_root, pretty_print=True, encoding='utf-8', xml_declaration=True))
