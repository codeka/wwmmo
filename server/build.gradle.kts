
buildscript {
  dependencies {
    classpath("org.hidetake:gradle-ssh-plugin:2.2.0")
  }
}

plugins {
  id("com.squareup.wire")
  id("kotlin")
  id("application")
}

repositories {
  mavenCentral()
}

wire {
  protoPath {
    srcDir("../common/src/main/proto")
  }
  sourcePath {
    srcDir("src/main/proto")
  }

  kotlin {
  }
}

application {
  mainClass.set("au.com.codeka.warworlds.server.Program")
}

distributions {
  main {
    contents {
      from("src/main/data") {
        into("data")
        exclude("store/**")
        exclude("cache/**")
        exclude("config-debug.json")

      }
    }
  }
}

//startScripts {
//  doLast {
//    // Remove too-long-classpath and use wildcard ( works for java 6 and above only)
//    windowsScript.text = windowsScript.text.replaceAll("set CLASSPATH=.*", "set CLASSPATH=.;%APP_HOME%/lib/*")
//  }
//}

task<Exec>("deploy") {
  dependsOn("installDist")
  workingDir("${buildDir}/install/server")
  commandLine("rsync", "-crlD", "--chmod=ugo=rwX", ".", "wwmmo@wwmmo.codeka.com.au:/home/wwmmo/staging")
}

dependencies {
  implementation("au.com.codeka:carrot:2.4.5")
  implementation("joda-time:joda-time:2.9.9")
  implementation("com.google.api-client:google-api-client:1.30.10")
  implementation("com.google.api-client:google-api-client-gson:1.30.10")
  implementation("com.google.code.findbugs:jsr305:3.0.2")
  implementation("com.google.code.gson:gson:2.8.9")
  implementation("com.google.firebase:firebase-admin:7.0.1")
  implementation("com.google.guava:guava:24.1-jre")
  implementation("com.squareup.wire:wire-runtime:4.4.1")
  implementation("org.simplejavamail:simple-java-mail:4.2.1")
  implementation("org.eclipse.jetty:jetty-server:9.4.27.v20200227")
  implementation("org.eclipse.jetty.websocket:websocket-server:9.4.0.v20161208")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
  implementation("org.xerial:sqlite-jdbc:3.36.0")
  implementation("com.patreon:patreon:0.4.2")
  implementation(project(":common"))
  implementation(project(":planet-render"))
}
