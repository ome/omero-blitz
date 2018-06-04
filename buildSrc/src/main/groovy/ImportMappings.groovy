import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ImportMappings extends DefaultTask {

    @OutputFile
    File extractDir

    @TaskAction
    void apply() {
        def omeroModelArtifact = project.configurations
                .collectMany { it.allDependencies }
                .findAll { it.name.contains("omero-model") ? it : null }

        if (omeroModelArtifact == null) {
            throw new GradleException('Can\'t find omero-model artifact')
        }



    }
}
