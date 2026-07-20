plugins {
    id("com.android.application")
}

android {
    namespace = "com.tripperdee.salmontracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tripperdee.salmontracker"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-polished-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("androidx.room:room-runtime:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
