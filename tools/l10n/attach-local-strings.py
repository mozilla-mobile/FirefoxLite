# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os
from lxml import etree

locale_folder = 'src/main/res'
customized_strings_file = 'strings_customized.xml'

source = etree.parse('src/main/res/values/strings.xml')
strings = [x.attrib['name'] for x in source.iter(tag='strings')]


for dirpath, _, _ in os.walk(locale_folder):
    dirname = dirpath.split(os.path.sep)[-1]
    if dirname.startswith('values-') and os.path.exists(os.path.join(dirpath, customized_strings_file)):
        print("LOCALE: %s" % dirname)

        customized = etree.parse(os.path.join(dirpath, customized_strings_file))
        customized_root = customized.getroot()

        locale = etree.parse(os.path.join(dirpath, 'strings.xml'), etree.XMLParser(strip_cdata=False))
        locale_root = locale.getroot()
        localized_strings = [x.attrib['name'] for x in locale_root.iter(tag='string')]

        for s in customized.iter(tag='string'):
            if s.attrib['name'] in localized_strings:
                print("Duplicated string: %s" % s.attrib['name'])
                customized_root.remove(s)

        with open(os.path.join(dirpath, customized_strings_file), 'w') as f:
            f.write(etree.tostring(customized_root, pretty_print=True, encoding='utf-8', xml_declaration=True))
