// mongo.gradle.kts

import org.gradle.api.tasks.StopExecutionException
import java.io.FileInputStream
import java.util.Properties

// Load mongo.properties
val mongoProps = loadProperties("config/mongo.properties")

// Function to load properties from a file
fun loadProperties(filePath: String): Properties {
    val properties = Properties()
    FileInputStream(filePath).use { properties.load(it) }
    return properties
}

// Task to start mongo Docker container
tasks.register<StartDockerContainer>("startMongoDB") {
    group = "MongoDB"
    description = "Starts the Mongo DB Docker container"
    
    // Define variables
    var databasePort: Int = mongoProps.getProperty("databasePort").toInt()
    var mongoUsername: String = mongoProps.getProperty("mongoUsername")
    var mongoPassword: String = mongoProps.getProperty("mongoPassword")

    //Set needed properties
    containerName = mongoProps.getProperty("containerName")
    imageName = mongoProps.getProperty("imageName")
    imageTag = mongoProps.getProperty("imageTag")

    portMap.put(databasePort, 27017)

    environmentMap.put("MONGO_INITDB_ROOT_USERNAME", mongoUsername)
    environmentMap.put("MONGO_INITDB_ROOT_PASSWORD", mongoPassword)

    volumeMap.put(mongoProps.getProperty("databaseVolumeName"),"/data/db")
}

tasks.register<StopDockerContainer>("stopMongoDB") {
    group = "MongoDB"
    description = "Stops the Mongo DB Docker container"

    containerName = mongoProps.getProperty("containerName")
}

tasks.named("startMongoDB") {
    mustRunAfter("stopMongoDB")
}

// Task to restart mongo Docker container
tasks.register("restartMongoDB") {
    group = "MongoDB"
    description = "Restarts the MongoDB Docker container"
    dependsOn("stopMongoDB", "startMongoDB")
}