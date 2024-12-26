import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class StartDockerContainer  @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val imageName: Property<String>

    @get:Input
    abstract val imageTag: Property<String>

    @get:Input
    abstract val containerName: Property<String>

    @get:Input
    abstract val portMap: MapProperty<Int, Int>

    @get:Input
    abstract val environmentMap: MapProperty<String, String>

    @get:Input
    abstract val volumeMap: MapProperty<String, String>

    @TaskAction
    fun startContainer() {
         DockerHelper(execOperations).startContainer(
            imageName.get(), imageTag.get(),
            containerName.get(), portMap.get(), environmentMap.get(), volumeMap.get())
    }
}
