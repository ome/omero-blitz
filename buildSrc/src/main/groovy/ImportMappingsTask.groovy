import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class ImportMappingsTask extends DefaultTask {

    static final String CONFIGURATION_NAME = 'mappings'
    static final String OMERO_MODEL_VERSION = '1.0.+'

    @OutputDirectory
    File extractDir

    @TaskAction
    void apply() {
        def omeroModelArtifact = project.plugins.hasPlugin(JavaPlugin) ?
                getCompileOmeroModel() :
                getWithConfig()

        if (omeroModelArtifact == null) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        project.copy {
            from project.zipTree(omeroModelArtifact.file).matching { include "**/*.ome.xml" }
            into extractDir
            include "mappings/**"
        }
    }

    def getCompileOmeroModel() {
        project.configurations.compile
                .resolvedConfiguration
                .resolvedArtifacts
                .find {
            item -> item.name.contains("omero-model")
        }
    }

    def getWithConfig() {
        def config = project.configurations[CONFIGURATION_NAME]
        if (config.dependencies.empty) {
            project.dependencies {
                mappings "org.openmicroscopy:omero-model:$OMERO_MODEL_VERSION"
            }
        }
        config
    }
}
