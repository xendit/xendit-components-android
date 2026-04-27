import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.vanniktech) // for maven boilerplate
//  `maven-publish` FOR JITPACK
}

group = "co.xendit"
version = "1.0.0"

android {
  namespace = "co.xendit.components"
  compileSdk = 37

  defaultConfig {
    minSdk = 26

    buildConfigField("String", "VERSION_NAME", "\"${project.version}\"")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro", "consumer-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll(
      "-Xjvm-default=all-compatibility",
      "-Xstring-concat=inline"
    )
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.material)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.okhttp)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.retrofit.converter.scalars)
  implementation(libs.retrofit)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.coil)
  implementation(libs.google.libphonenumber)
  implementation(libs.zxing.core)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp.profiler)
  releaseImplementation(libs.okhttp.requests.modifier.no.op)
  debugImplementation(libs.okhttp.requests.modifier.debug)
  // Standard Test Dependencies
  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// LATEST VANNIKTECH CONFIGURATION (v0.36.0)
mavenPublishing {
  configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
    javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
    sourcesJar = com.vanniktech.maven.publish.SourcesJar.Sources(),
    variant = "release",
  ))
  // Use coordinates to set group, artifact, and version
  coordinates(group.toString(), "components", version.toString())

  // Enable Maven Central and GPG signing
  publishToMavenCentral()
  val signingEnabled = (project.findProperty("signingEnabled") as String?)?.toBoolean() ?: true
  if (signingEnabled) {
    signAllPublications()
  }

  pom {
    name.set("Xendit Payment SDK")
    description.set("Jetpack Compose Payment SDK for Xendit.")
    url.set("https://github.com/xendit/xendit-components-android")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("xenditdeveloper")
        name.set("Xendit")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/xendit/xendit-components-android.git")
      url.set("https://github.com/xendit/xendit-components-android")
    }
  }
}
