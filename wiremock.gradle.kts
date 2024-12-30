// wiremock.gradle.kts

import org.gradle.api.tasks.StopExecutionException
import java.io.FileInputStream
import java.util.Properties

// Load wiremock.properties
val wiremockProps = loadProperties("config/wiremock.properties")

// Function to load properties from a file
fun loadProperties(filePath: String): Properties {
    val properties = Properties()
    FileInputStream(filePath).use { properties.load(it) }
    return properties
}

// Task to start mongo Docker container
tasks.register<StartDockerContainer>("startWiremock") {
    group = "Wiremock"
    description = "Starts the Wiremock container"
    
    // Define variables
    var webPort: Int = wiremockProps.getProperty("webPort").toInt()

    //Set needed properties
    containerName = wiremockProps.getProperty("containerName")
    repositoryName = wiremockProps.getProperty("repositoryName")
    imageName = wiremockProps.getProperty("imageName")
    imageTag = wiremockProps.getProperty("imageTag")

    portMap.put(webPort, 8080)

    volumeMap.put(wiremockProps.getProperty("wiremockPath"),"/home/wiremock")
}

tasks.register<StopDockerContainer>("stopWiremock") {
    group = "wiremock"
    description = "Stops the Wiremock Docker container"

    containerName = wiremockProps.getProperty("containerName")
}

tasks.named("startWiremock") {
    mustRunAfter("stopWiremock")
}

// Task to restart mongo Docker container
tasks.register("restartWiremock") {
    group = "wiremock"
    description = "Restarts the Wiremock Docker container"
    dependsOn("stopWiremock", "startWiremock")
}