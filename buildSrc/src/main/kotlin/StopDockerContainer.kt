import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class StopDockerContainer  @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val containerName: Property<String>


    @TaskAction
    fun stopContainer() {
         DockerHelper(execOperations).stopContainer(containerName.get())
    }
}
