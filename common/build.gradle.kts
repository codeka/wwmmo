
plugins {
  id("kotlin")
  id("com.squareup.wire")
}

repositories {
  mavenCentral()
}

tasks.withType<JavaCompile> {
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

wire {
  kotlin {
  }
}

tasks.test {
  testLogging {
    // Show that tests are run in the command-line output
    events("passed")
  }
  useJUnitPlatform()
}

// See https://github.com/square/wire/issues/1123
// Wire does not add the generated file to the source set, so we do it manually.0
sourceSets {
  all {
    kotlin.srcDir("$buildDir/generated/source/wire")
  }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
  implementation("com.squareup.wire:wire-runtime:4.4.1")
  implementation("com.google.code.findbugs:jsr305:3.0.2")
  implementation("com.google.guava:guava:24.1-android")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:5.0.1")
  testImplementation("io.kotest:kotest-assertions-core-jvm:5.0.1")
}
