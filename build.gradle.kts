// build.gradle.kts

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	id("de.surala.containertool.docker-plugin") version "1.0.0"
}

group = "de.surala.example.eco"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

repositories {
	mavenCentral()
}

tasks.test {
	useJUnitPlatform()
}


dependencies {
	// Existing dependencies
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Testing dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.mockito", module = "mockito-core")
		exclude(group = "org.mockito", module = "mockito-junit-jupiter")
	}
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.amqp:spring-rabbit-test")
	testImplementation("com.ninja-squad:springmockk:4.0.2")

	// Testcontainer
	testImplementation("org.testcontainers:junit-jupiter:1.20.0")
	testImplementation("org.testcontainers:mongodb:1.20.0")
	testImplementation("org.testcontainers:rabbitmq:1.20.0")

	// JUnit Platform Launcher
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

docker {
	mainGroupName = "Docker"

	container {
		group = "Database"
		name = "mongoDB"
		image = "mongo"
		tag = "latest"
		ports[27017] = 27017
		environments["MONGO_INITDB_ROOT_USERNAME"] = "admin"
		environments["MONGO_INITDB_ROOT_PASSWORD"] = "secret"
		volumes["mongo-data"] = "/data/db"
	}

	container {
		group = "Mock"
		name = "wiremock"
		image = "wiremock/wiremock"
		tag = "latest"
		ports[8089] = 8080
		volumes[".\\wiremock"] = "/home/wiremock"
	}

	container {
		group = "RabbitMQ"
		name = "rabbitmq"
		image = "rabbitmq"
		tag = "3-management"
		ports[5672] = 5672
		ports[15672] = 15672
		environments["RABBITMQ_DEFAULT_USER"] = "guest"
		environments["RABBITMQ_DEFAULT_PASS"] = "guest"
	}
}