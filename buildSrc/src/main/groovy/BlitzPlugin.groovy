import dslplugin.DslPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

class BlitzPlugin implements Plugin<Project> {

    static final String DEFAULT_IMPORT_MAPPINGS_DIR = 'extracted'
    static final String OMERO_MODEL_VERSION = '1.0.+'

    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin(DslPlugin)) {
            throw new GradleException("Requires org.openmicroscopy:dslplugin")
        }

        // Add the 'blitz' extension object
        def blitz = project.extensions.create('blitz', BlitzExtension)
        configureImportMappingsTask(project, blitz)
        configureCombineTask(project, blitz)
    }

    void configureImportMappingsTask(Project project, BlitzExtension blitz) {
        def mappingsDir = blitz.getMappingsDir()
        if (mappingsDir == null) {
            mappingsDir = project.file( "${project.buildDir}/${DEFAULT_IMPORT_MAPPINGS_DIR}")
        }

        project.task('importMappings', type: ImportMappingsTask) {
            description 'Extracts mapping files from dependency org.openmicroscopy:omero-model'
            extractDir = mappingsDir
        }
    }

    void configureCombineTask(Project project, BlitzExtension blitz) {
        project.task('hello', type: CombinedTask) {

        }
    }
}

class BlitzExtension {

    String[] languages

    File mappingsDir

    File outputPath


}