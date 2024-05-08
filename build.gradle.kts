plugins {
	id("java")
}

group = "dev.noeul.roomofprey"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
}

tasks.jar {
	from("LICENSE.txt")
}
