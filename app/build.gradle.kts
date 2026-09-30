import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("io.github.takahirom.roborazzi")
}

val roborazziVersion = "1.43.0"

android {
    namespace = "com.classrecord.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.classrecord.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.5.1"
        val buildTimeIso = Instant.now().toString()
        buildConfigField("String", "BUILD_TIME_ISO", "\"$buildTimeIso\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ""
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    outputDir.set(file("build/homepage-screenshots-raw"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("androidx.lifecycle:lifecycle-runtime-testing:${libs.versions.lifecycle.get()}")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:$roborazziVersion")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:$roborazziVersion")
}

tasks.register<Exec>("exportHomepageScreenshots") {
    group = "verification"
    description = "Convert Roborazzi PNG captures to WebP under docs/screenshots/homepage/"
    dependsOn("recordRoborazziDebug")
    commandLine("python3", "${rootProject.projectDir}/scripts/export-homepage-screenshots.py")
}
