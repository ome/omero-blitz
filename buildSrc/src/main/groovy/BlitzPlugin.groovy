import dslplugin.DslPlugin
import dslplugin.DslTask
import dslplugin.VelocityExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = 'omero'

    static final String DEFAULT_IMPORT_MAPPINGS_DIR = 'extracted'
    static final String DEFAULT_COMBINED_DIR = 'combined'

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin(DslPlugin)) {
            throw new GradleException("Requires org.openmicroscopy:dslplugin")
        }

        // Add the 'blitz' extension object
        blitzExt = project.extensions.create('blitz', BlitzExtension)
        configureImportMappingsTask(project)
        configureCombineTask(project)
        configureSplitTask(project)
    }

    def configureImportMappingsTask(Project project) {
        project.afterEvaluate {
            def mappingsDir = getMappingsDir(project, blitzExt)

            project.task('importMappings', type: ImportMappingsTask) {
                group = GROUP
                description 'Extracts mapping files from dependency org.openmicroscopy:omero-model'
                extractDir = mappingsDir
            }
        }
    }

    def configureCombineTask(Project project) {
        project.afterEvaluate {
            def mappingsDir = getMappingsDir(project, blitzExt)

            // Config for velocity
            def velocityExt = new VelocityExtension()
            velocityExt.resource_loader = 'file'
            velocityExt.logger_class_name = project.getLogger().getClass().getName()

            // Add task to process combine.vm
            project.task('processCombine', type: DslTask) {
                group = GROUP
                description 'Processes the combined.vm'

                // Configure velocity
                velocityProps = DslPlugin.configureVelocity(velocityExt)
                template = getCombinedTemplateFile()
                omeXmlFiles = project.fileTree(dir: mappingsDir, include: '**/*.ome.xml')
                outputPath = project.file(getCombinedDir(project, blitzExt))
                formatOutput = { st -> "${st.getShortname()}I.combined" }
            }
        }
    }

    def configureSplitTask(Project project) {
        project.afterEvaluate {
            // Check only supported languages are used
            def langs = blitzExt.languages.collect() {
                def val = Language.find(it)
                if (val == null) {
                    throw new GradleException("Unsupported language")
                }
                return val
            }

            project.task("splitCombined", type: SplitTask) {
                group = GROUP
                languages = langs
                outputDir = blitzExt.outputPath
                combinedDir = getCombinedDir(project, blitzExt)
            }
        }
    }

    def getCombinedTemplateFile() {
        return new File(this.class.classLoader.getResource('combined.vm').getFile())
    }

    static def getMappingsDir(Project project, BlitzExtension blitz) {
        def mappingsDir = blitz.getMappingsDir()
        if (mappingsDir == null) {
            mappingsDir = project.file("${project.buildDir}/${DEFAULT_IMPORT_MAPPINGS_DIR}")
        }
        return mappingsDir
    }

    static File getCombinedDir(Project project, BlitzExtension blitz) {
        return new File("${project.buildDir}/${DEFAULT_COMBINED_DIR}")
    }
}

