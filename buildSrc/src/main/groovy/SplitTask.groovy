import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.RegExpNameMapper
import org.gradle.api.tasks.*

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
    FileCollection combined

    @Optional
    Transformer<String, String> nameTransformer

    /**
     * Directory to spit out source files
     * @param dir
     * @return
     */
    def outputDir(File dir) {
        this.outputDir = dir
    }

    /**
     * Directory to spit out source files
     * @param dir
     * @return
     */
    def outputDir(String dir) {
        this.outputDir = new File(dir)
    }

    /**
     * Custom set method for concatenating FileCollections
     * @param combinedFiles
     */
    def combined(FileCollection combinedFiles) {
        this.combined = this.combinedFiles + combinedFiles
    }

    def rename(Pattern sourceRegEx, String replaceWith) {
        this.nameTransformer = new RegExpNameMapper(
                sourceRegEx,
                FilenameUtils.removeExtension(replaceWith)
        )
    }

    def rename(String sourceRegEx, String replaceWith) {
        this.nameTransformer = new RegExpNameMapper(
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
            // Transform prefix enum to lower case for naming
            final def prefixName = prefix.name().toLowerCase()

            // Assign default to rename
            if (!nameTransformer) {
                nameTransformer = new RegExpNameMapper('(.*?)I[.]combined',
                        "\$1I${prefix.extension}")
            }

            project.copy { c ->
                c.from combined
                c.into outputDir
                c.rename nameTransformer
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
