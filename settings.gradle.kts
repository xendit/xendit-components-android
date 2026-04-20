import java.util.Properties
import java.io.File

val localPropsFile = File(rootDir, "local.properties")
if (localPropsFile.exists()) {
  val props = Properties()
  localPropsFile.inputStream().use { props.load(it) }
  fun setGradleProp(key: String) {
    val v = props.getProperty(key) ?: return
    val full = "org.gradle.project.$key"
    if (System.getProperty(full) == null) {
      System.setProperty(full, v)
    }
  }
  listOf(
    "mavenCentralUsername",
    "mavenCentralPassword",
    "signing.keyId",
    "signing.password",
    "signing.secretKeyRingFile",
    "signingInMemoryKey",
    "signingInMemoryKeyPassword",
    "mavenCentralHost"
  ).forEach(::setGradleProp)
}

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
//    mavenLocal() //to use maven local
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "XenditComponentsAndroid"
include(":exampleApp")
include(":paymentsdk") // comment this if use jitpack
