import dslplugin.DslOperation
import dslplugin.DslPlugin
import dslplugin.DslTask
import dslplugin.VelocityExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    final def GROUP = 'omero'

    static final String DEFAULT_IMPORT_MAPPINGS_DIR = 'extracted'
    static final String OMERO_MODEL_VERSION = '1.0.+'

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
        def mappingsDir = getMappingsDir(project, blitzExt)

        // Config for velocity
        def velocityExtension = new VelocityExtension()
        velocityExtension.resource_loader = 'file'
        velocityExtension.file_resource_loader_path = "src/main/resources/templates"
        velocityExtension.file_resource_loader_cache = false
        velocityExtension.logger_class_name = project.getLogger().getClass().getName()

        // Default operation config
        def dslOp = new DslOperation()
        dslOp.template = "combined.vm"
        dslOp.outputPath = project.file("src/generated/combined")
        dslOp.omeXmlFiles = project.fileTree(dir: mappingsDir, include: '**/*.ome.xml')
        dslOp.formatOutput = { st -> "${st.getShortname()}I.combined" }

        // Add task to process combine.vm
        project.task('processCombine', type: DslTask) {
            group = GROUP
            description 'Processes the combined.vm'

            // Configure velocity
            properties = DslPlugin.configureVelocity(velocityExtension)
            template = "combined.vm"
            omeXmlFiles = info.omeXmlFiles
            outputPath = info.outputPath
            outFile = info.outFile
            formatOutput = info.formatOutput
        }
    }

    def createTasksForSplits(Project project) {

    }

    static def getMappingsDir(Project project, BlitzExtension blitz) {
        def mappingsDir = blitz.getMappingsDir()
        if (mappingsDir == null) {
            mappingsDir = project.file("${project.buildDir}/${DEFAULT_IMPORT_MAPPINGS_DIR}")
        }
        return mappingsDir
    }
}

class BlitzExtension {

    String[] languages

    File mappingsDir

    File outputPath


}