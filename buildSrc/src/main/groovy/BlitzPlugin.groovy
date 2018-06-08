import dslplugin.DslPluginBase
import dslplugin.DslTask
import dslplugin.VelocityExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = 'omero'

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        // Apply the base plugin, it's all we need
        project.plugins.apply(DslPluginBase)

        // Add the 'blitz' extension object
        blitzExt = project.extensions.create('blitz', BlitzExtension)

        // Default config for blitz
        blitzExt.mappingsDir = new File("${project.buildDir}/extracted")
        blitzExt.combinedDir = new File("${project.buildDir}/combined")

        // Add container for blitz
        blitzExt.extensions.add('interfaces', project.container(SplitExtension))

        configureImportMappingsTask(project)
        configureCombineTask(project)
        configureSplitTasks(project)
    }

    def configureImportMappingsTask(Project project) {
        def task = project.task('importMappings', type: ImportMappingsTask) {
            group = GROUP
            description 'Extracts mapping files from dependency org.openmicroscopy:omero-model'
        }

        project.afterEvaluate {
            task.extractDir = blitzExt.mappingsDir
        }
    }

    def configureCombineTask(Project project) {
        def task = project.task('processCombine', type: DslTask) {
            group = GROUP
            description 'Processes the combined.vm'
        }

        // Config for velocity
        VelocityExtension ve = project.dsl.velocity
        ve.file_resource_loader_path = "buildSrc/src/main/resources"

        // Create properties
        Properties props = DslPluginBase.createVelocityProperties(ve)

        project.afterEvaluate {
            // Add task to process combine.vm
            // Configure velocity
            task.velocityProps = props
            task.template = new File("combined.vm")
            task.omeXmlFiles = project.fileTree(dir: blitzExt.mappingsDir, include: '**/*.ome.xml')
            task.outputPath = blitzExt.combinedDir
            task.formatOutput = { st -> "${st.getShortname()}I.combined" }
        }

        // project.tasks.findByName('importMappings').dependsOn(task.name)
    }

    def configureSplitTasks(Project project) {
        project.dsl.generate.all { SplitExtension split ->
            def taskName = "split${split.name.capitalize()}"

            // Create task and assign group name
            def task = project.task(taskName, type: SplitTask) {
                group = GROUP
                description = "Splits ${split.language} from .combined files"
            }

            // Assign property values to task inputs
            project.afterEvaluate {
                task.combinedDir = blitzExt.combinedDir
                task.language = split.language
                task.outputDir = split.outputDir
            }

            if (project.plugins.hasPlugin(JavaPlugin)) {
                // Ensure the dsltask runs before compileJava
                project.tasks.getByName("compileJava").dependsOn(taskName)
            }
        }
    }
}

