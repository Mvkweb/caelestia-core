plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.5.1"
}

group = "com.caelestia"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.lucko.me/") }
}

dependencies {
    // PaperMC API
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    
    // Kotlin standard library is automatically added by the kotlin plugin in newer versions, 
    // but JDA is needed
    implementation("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }
    compileOnly("me.lucko:spark-api:0.1-SNAPSHOT")
    
    // SnakeYAML is already provided by PaperMC but keeping it clean for ide resolution if needed,
    // although we will just use Bukkit's built-in YamlConfiguration.
}

kotlin {
    jvmToolchain(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    
    shadowJar {
        archiveClassifier.set("") // Remove -all suffix
        
        // Relocate JDA and its dependencies to avoid conflicts with other plugins
        relocate("net.dv8tion", "com.caelestia.libs.net.dv8tion")
        relocate("com.neovisionaries", "com.caelestia.libs.com.neovisionaries")
        relocate("com.fasterxml", "com.caelestia.libs.com.fasterxml")
        relocate("okhttp3", "com.caelestia.libs.okhttp3")
        relocate("okio", "com.caelestia.libs.okio")
        relocate("org.apache.commons.collections4", "com.caelestia.libs.org.apache.commons.collections4")
        relocate("gnu.trove", "com.caelestia.libs.gnu.trove")
        
        mergeServiceFiles() // Critical for JDA and Jackson
    }
}
