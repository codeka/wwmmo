import java.io.FileInputStream
import java.util.Properties

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("com.google.gms.google-services")
}

var signingProps = Properties()
var signingPropsFile = File("client/signing.properties")
if (signingPropsFile.isFile()) {
  signingProps.load(FileInputStream(signingPropsFile))
}

fun getVersionCodeFromGit(): Int {
  // Same as running:
  // git rev-list HEAD | wc -l
  val proc = ProcessBuilder("git", "rev-list", "HEAD")
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
  return proc.inputStream.bufferedReader().readText().split("\n").size
}

android {
  compileSdk = 34
  buildToolsVersion = "33.0.0"

  defaultConfig {
    applicationId = "au.com.codeka.warworlds2"
    minSdk = 21
    targetSdk = 34
    versionCode = getVersionCodeFromGit()
    versionName = "2.0"
    multiDexEnabled = true
    vectorDrawables.useSupportLibrary = true
  }
  buildFeatures {
    viewBinding = true
  }
  sourceSets {
    getByName("main").java.srcDirs(File("src/main/kotlin"))
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  signingConfigs {
    create("release") {
      storeFile = File(signingProps["keystore"] as String? ?: "/tmp/non-existant")
      storePassword = signingProps["password"] as String?
      keyAlias = signingProps["alias"] as String?
      keyPassword = signingProps["password"] as String?
    }
  }

  configurations {
    all {
      // This seems like a weird bug... something else includes listenablefuture from guava.
      exclude(group = "com.google.guava", module = "listenablefuture")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")

      buildConfigField("String", "DEFAULT_SERVER", "\"http://game2.war-worlds.com/\"")
    }
    debug {
      buildConfigField("String", "DEFAULT_SERVER", "\"http://127.0.0.1:8080/\"")
    }
  }
  namespace = "au.com.codeka.warworlds.client"
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
  implementation("androidx.preference:preference-ktx:1.2.1")
  implementation("androidx.transition:transition:1.4.1")
  implementation("com.google.android.material:material:1.11.0-alpha03")
  implementation("com.google.android.gms:play-services-base:18.2.0")
  implementation("com.google.android.gms:play-services-auth:20.7.0")
  implementation("com.google.firebase:firebase-core:21.1.1")
  implementation("com.google.firebase:firebase-messaging:23.2.1")
  implementation("com.google.guava:guava:24.1-android")
  implementation("com.squareup.picasso:picasso:2.71828")
  implementation("com.squareup.wire:wire-runtime:4.4.1")
  implementation("com.android.support:multidex:1.0.3")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")
  implementation(project(":common"))
}
