import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.*

class SplitTask extends Copy {

    /**
     * List of the languages we want to split from .combined files
     */
    @Input
    Language[] languages

    /**
     * Directory to spit out source files
     */
    @OutputDirectory
    File outputDir

    /**
     * Directory to .combined files
     */
    @InputDirectory
    File combinedDir

    @Internal
    CopySpec internalCopySpec

    @TaskAction
    void action() {
        internalCopySpec = project.copySpec {
            from(combinedDir) {
                include '**/*.combined'
            }
        }

        switch (languages) {
            case Language.CPP:
                cppSplit()
        }
    }

    def cppSplit() {
        Language.CPP.prefixes.each { prefix ->
            final def prefixName = prefix.name().toLowerCase()

            project.copy {
                into "${outputDir}/${prefixName}"
                with internalCopySpec
                filter { filerLine(it, prefixName) }
                rename '(.*?)I[.]combined', "omero/model/\$1I.${prefix.extension}"
            }
        }
    }

    static def filerLine(String line, String prefix) {
        return line.matches("^\\[all](.*)|^\\[${prefix}](.*)") ?
                line.replaceAll("^\\[all]|^\\[${prefix}]", "") :
                null
    }
}
