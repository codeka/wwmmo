buildscript {
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.1.2")
    classpath("com.google.gms:google-services:4.4.0")
    classpath("com.squareup.wire:wire-gradle-plugin:4.4.1")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  }
}

allprojects {
  repositories {
    mavenCentral()
    maven(url = "https://maven.fabric.io/public")
    google()
  }
}
