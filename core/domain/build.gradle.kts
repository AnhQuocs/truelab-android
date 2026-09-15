plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":core:algorithm"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
