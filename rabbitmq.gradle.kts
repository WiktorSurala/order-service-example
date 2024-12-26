// rabbitmq.gradle.kts

import org.gradle.api.tasks.StopExecutionException
import java.io.FileInputStream
import java.util.Properties

// Load rabbitmq.properties
val rabbitmqProps = loadProperties("config/rabbitmq.properties")

// Function to load properties from a file
fun loadProperties(filePath: String): Properties {
    val properties = Properties()
    FileInputStream(filePath).use { properties.load(it) }
    return properties
}

// Task to start RabbitMQ Docker container
tasks.register<StartDockerContainer>("startRabbitMQ") {
    group = "RabbitMQ"
    description = "Starts the RabbitMQ Docker container"

    // Define variables
    var rabbitmqAMQPPort: Int = rabbitmqProps.getProperty("amqpPort").toInt()
    var rabbitmqManagementPort: Int = rabbitmqProps.getProperty("managementPort").toInt()
    var rabbitmqUsername: String = rabbitmqProps.getProperty("rabbitmqUsername")
    var rabbitmqPassword: String = rabbitmqProps.getProperty("rabbitmqPassword")


    //Set needed properties
    containerName = rabbitmqProps.getProperty("containerName")
    imageName = rabbitmqProps.getProperty("imageName")
    imageTag = rabbitmqProps.getProperty("imageTag")

    portMap.put(rabbitmqAMQPPort, 5672)
    portMap.put(rabbitmqManagementPort, 15672)

    environmentMap.put("RABBITMQ_DEFAULT_USER", rabbitmqUsername)
    environmentMap.put("RABBITMQ_DEFAULT_PASS", rabbitmqPassword)
}

tasks.register<StopDockerContainer>("stopRabbitMQ") {
    group = "RabbitMQ"
    description = "Stops the RabbitMQ Docker container"

    containerName = rabbitmqProps.getProperty("containerName")
}

tasks.named("startRabbitMQ") {
    mustRunAfter("stopRabbitMQ")
}

// Task to restart RabbitMQ Docker container
tasks.register("restartRabbitMQ") {
    group = "RabbitMQ"
    description = "Restarts the RabbitMQ Docker container"
    dependsOn("stopRabbitMQ", "startRabbitMQ")
}