import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.RegExpNameMapper
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class SplitTask extends DefaultTask {

    /**
     * List of the languages we want to split from .combined files
     */
    @Input
    String language

    /**
     * Directory to spit out source files
     */
    @OutputFiles
    File outputDir

    /**
     * Collection of .combined files to process
     */
    @SkipWhenEmpty
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    FileCollection combinedFiles

    @SkipWhenEmpty
    @Input
    private Transformer<String, String> rename

    /**
     * Custom set method for concatenating FileCollections
     * @param combinedFiles
     */
    def combinedFiles(FileCollection combinedFiles) {
        this.combinedFiles = this.combinedFiles + combinedFiles
    }

    def rename(Pattern sourceRegEx, String replaceWith) {
        this.rename = new RegExpNameMapper(sourceRegEx, replaceWith)
    }

    def rename(String sourceRegEx, String replaceWith) {
        this.rename = new RegExpNameMapper(
                sourceRegEx,
                FilenameUtils.removeExtension(replaceWith)
        )
    }

    @TaskAction
    void action() {
        // Lookup the language
        Language lang = Language.find(language)
        if (lang == null) {
            throw new GradleException("Unsupported language : ${language}")
        }

        lang.prefixes.each { Prefix prefix ->
            // Transform prefix enum to lower case for nameing
            final def prefixName = prefix.name().toLowerCase()

            // Assign default to rename
            if (!this.rename) {
                rename('(.*?)I[.]combined', "\$1I.${prefix.extension}")
            }

            project.copy { c ->
                c.from combinedFiles
                c.into outputDir
                c.rename rename
                c.filter { String line -> filerLine(line, prefixName) }
            }
        }
    }

    static def filerLine(String line, String prefix) {
        return line.matches("^\\[all](.*)|^\\[${prefix}](.*)") ?
                line.replaceAll("^\\[all]|^\\[${prefix}]", "") :
                null
    }
}
