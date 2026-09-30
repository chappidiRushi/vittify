import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.reddy.vittify"
    compileSdk = 36
    
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.reddy.vittify"
        minSdk = 26
        targetSdk = 36
        versionCode = 94
        versionName = "2.1.61-beta"
        manifestPlaceholders["appName"] = "@string/app_name"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load RSA public key from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val localProperties = Properties()
            localProperties.load(localPropertiesFile.inputStream())
            
            val rsaPublicKey = localProperties.getProperty("RSA_PUBLIC_KEY", "")
            buildConfigField("String", "RSA_PUBLIC_KEY", "\"$rsaPublicKey\"")
        } else {
            // Fallback empty key for CI/CD builds
            buildConfigField("String", "RSA_PUBLIC_KEY", "\"\"")
        }
        

    }


    signingConfigs {
        create("release") {
            // Default to values from local.properties if available
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                val localProperties = Properties()
                localProperties.load(localPropertiesFile.inputStream())
                
                val keystorePath = localProperties.getProperty("RELEASE_STORE_FILE", "")
                if (keystorePath.isNotEmpty()) {
                    storeFile = file(keystorePath)
                    storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
                    keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
                    keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
                }
            }
        }
    }
    
    flavorDimensions += "version"
    productFlavors {

        create("standard") {
            dimension = "version"
            isDefault = true
            // Standard flavor includes all architectures (including x86 for emulators)
        }
    }

   splits {
       abi {
           // Disable splits for bundle builds (AABs).
           //noinspection WrongGradleMethod
           val runTasks = gradle.startParameter.taskNames.map { it.lowercase() }
           //noinspection WrongGradleMethod
           val isBundleBuild = runTasks.any { it.contains("bundle") }

           isEnable = !isBundleBuild

           reset()
           include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
           isUniversalApk = true
       }
   }


    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appName"] = "vittify beta"
        }
        release {
            manifestPlaceholders["appName"] = "@string/app_name"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // Include debug symbols for native crashes
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "org/bouncycastle/**",
                "META-INF/versions/9/OSGI-INF/**",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}


// Configure Room schema export
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.compose.animation)

    // Local modules
    implementation(project(":parser-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Color Picker for Compose
    implementation(libs.colorpicker.compose)
    implementation(libs.haze)
    
    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)
    
    // Lifecycle and ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Ktor for HTTP requests
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // Gson for backup/restore
    implementation(libs.gson)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Biometric Authentication
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.nearby)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
    implementation(libs.stream.webrtc.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // Hilt WorkManager integration
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    
    // LiteRT-LM for on-device LLM inference


    
    // Google Play In-App Updates (only for standard flavor)
    "standardImplementation"(libs.app.update)
    "standardImplementation"(libs.app.update.ktx)
    
    // Google Play In-App Reviews (only for standard flavor)
    "standardImplementation"(libs.review)
    "standardImplementation"(libs.review.ktx)
    
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // OpenCSV for CSV export
    implementation(libs.opencsv)
    testImplementation(kotlin("test"))

    // coil for images
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Compose Charts
    implementation(libs.compose.charts)

    // Reorderable
    implementation(libs.reorderable)

    // PDF Box for Android
    implementation(libs.pdfbox.android)
}
