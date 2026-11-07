import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("org.jetbrains.intellij.platform")
}

group = "com.max.medvedev"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
  jvmToolchain(21)
}

dependencies {
  intellijPlatform {
    intellijIdea(providers.gradleProperty("platformVersion")) {
      useInstaller = false
    }
    bundledPlugins("com.intellij.mcpServer", "org.jetbrains.plugins.terminal")
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.opentest4j:opentest4j:1.3.0")
  // The listener of the platform test framework loads junit.framework.TestCase.
  testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = providers.gradleProperty("pluginSinceBuild")
      untilBuild = provider { null }
    }
  }
}

tasks.test {
  useJUnitPlatform()
}
