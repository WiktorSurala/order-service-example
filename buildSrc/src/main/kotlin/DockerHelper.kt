import org.gradle.api.tasks.StopExecutionException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream

class DockerHelper(private val execOperations: ExecOperations) {
    private fun execCommand(commandList: List<String>): String {
        println("Executing command: ${commandList.joinToString(" ")}")
        val outputStream = ByteArrayOutputStream()
        execOperations.exec {
            commandLine = commandList
            standardOutput = outputStream
        }
        return outputStream.toString(Charsets.UTF_8).trim()
    }

    fun getContainerStatus(containerName: String): String {
        return execCommand(
            listOf(
                "docker",
                "container",
                "ps",
                "--all",
                "--filter", "name=$containerName",
                "--format", "{{.Status}}"
            )
        )
    }

    fun startContainer(
        repositoryName: String?,
        imageName: String,
        imageTag: String,
        containerName: String,
        portMap: Map<Int, Int>,
        environmentMap: Map<String, String?>,
        volumeMap: Map<String, String>
    ) {
        val defaultWorkingDir = System.getProperty("user.dir")
        println("Default Working Directory: $defaultWorkingDir")

        val containerStatus = getContainerStatus(containerName)
        if (containerStatus.isEmpty()) {
            println("$containerName container not found. Starting a new container...")
            val commandLineArguments = mutableListOf(
                "docker", "run", "-d",
                "--name", containerName
            )

            for ((key, value) in portMap) {
                commandLineArguments.add("-p")
                commandLineArguments.add("$key:$value")
            }

            for ((key, value) in environmentMap) {
                commandLineArguments.add("-e")
                commandLineArguments.add("$key=$value")
            }

            for ((key, value) in volumeMap) {
                commandLineArguments.add("-v")
                commandLineArguments.add("$key:$value")
            }

            val imageReference = if (repositoryName.isNullOrBlank()) {
                "$imageName:$imageTag"
            } else {
                "$repositoryName/$imageName:$imageTag"
            }
            commandLineArguments.add(imageReference)

            execCommand(commandLineArguments)
        } else if (containerStatus.contains("Exited")) {
            println("$containerName container exists but is not running. Starting the container...")
            execCommand(listOf("docker", "container", "start", containerName))
        } else {
            println("$containerName container is already running.")
        }

        // Verify if Container started successfully
        val checkContainerStatus = getContainerStatus(containerName)
        if (checkContainerStatus.contains("Up")) {
            println("$containerName started successfully.")
        } else {
            throw StopExecutionException("$containerName had issues starting: $containerStatus")
        }
    }

    fun stopContainer(containerName: String) {
        val exists = getContainerStatus(containerName)
        println("$containerName Status: ${exists.trim()}")
        if (exists.isNotEmpty()) {
            println("Stopping $containerName container...")
            execCommand(listOf("docker", "container", "stop", containerName))
            println("Removing $containerName container...")
            execCommand(listOf("docker", "container", "remove", containerName))
        } else {
            println("$containerName container does not exist.")
        }
    }
}