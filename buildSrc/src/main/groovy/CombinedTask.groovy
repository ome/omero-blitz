import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CombinedTask extends DefaultTask {

    @TaskAction
    void apply() {
        project.dsl() {
            velocity {
                resource_loader = 'file'
                file_resource_loader_path = "src/main/resources/templates"
                file_resource_loader_cache = false
            }
        }
    }
}
