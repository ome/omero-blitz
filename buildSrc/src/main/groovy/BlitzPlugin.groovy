import dslplugin.DslPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class BlitzPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(DslPlugin)) {

        }
    }
}
