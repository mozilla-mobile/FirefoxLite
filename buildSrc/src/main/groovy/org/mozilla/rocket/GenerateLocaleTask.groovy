package org.mozilla.rocket

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public class GenerateLocaleTask extends DefaultTask {
    String directory
    String[] locales

    @TaskAction
    def generateFile() {
        def generatedLocaleListFilename = 'LocaleList.java'
        def dir = project.file((Object)directory)
        dir.mkdir()
        def localeList = project.file(new File(dir, generatedLocaleListFilename))

        localeList.delete()
        localeList.getParentFile().mkdirs()
        localeList.createNewFile()
        localeList << "package org.mozilla.focus.generated;" << "\n" << "\n"
        localeList << "import java.util.Arrays;" << "\n"
        localeList << "import java.util.Collections;" << "\n"
        localeList << "import java.util.List;" << "\n"
        localeList << "\n"
        localeList << "public class LocaleList {" << "\n"
        // findbugs doesn't like "public static final String[]", see http://findbugs.sourceforge.net/bugDescriptions.html#MS_MUTABLE_ARRAY
        localeList << "    public static final List<String> BUNDLED_LOCALES = Collections.unmodifiableList(Arrays.asList(new String[] { "
        localeList << locales.join(", ") + " }));" << "\n"
        localeList << "}" << "\n"
    }
}