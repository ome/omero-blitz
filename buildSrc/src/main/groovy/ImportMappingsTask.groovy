import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class ImportMappingsTask extends DefaultTask {

    @OutputDirectory
    File extractDir

    @TaskAction
    void apply() {
        def omeroModelArtifact = project.configurations.compile
                .resolvedConfiguration
                .resolvedArtifacts
                .find {
            item -> item.name.contains("omero-model")
        }

        if (omeroModelArtifact == null) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        project.copy {
            from project.zipTree(omeroModelArtifact.file).matching { include "**/*.ome.xml" }
            into extractDir
            include "mappings/**"
        }
    }
}
