import dslplugin.DslPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class CombinedTask extends DefaultTask {

    @OutputDirectory
    File outputDir

    @TaskAction
    void apply() {
        if (!project.plugins.hasPlugin(DslPlugin)) {
            throw new GradleException("Requires org.openmicroscopy:dslplugin")
        }

        def output = getOutputDir()
        if (output == null) {
            outputDir = "${project.buildDir}/combined"
        }



        project.tasks ( ) {

        }

        project.dsl {
            velocity {
                resource_loader = 'file'
                file_resource_loader_path = "src/main/resources/templates"
                file_resource_loader_cache = false
            }

            combined {
                template = "combined.vm"
                outputPath = project.file(outputDir)
                omeXmlFiles = mappingsTree
                formatOutput = { st ->
                    "${st.getShortname()}I.combined"
                }
            }
        }
    }
}
