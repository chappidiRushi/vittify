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
        versionCode = (project.findProperty("app.versionCode") as? String)?.toIntOrNull() ?: 94
        versionName = (project.findProperty("app.versionName") as? String) ?: "2.1.61"

        val isBeta = (project.findProperty("app.isBeta") as? String)?.toBoolean()
            ?: (project.findProperty("app.versionName") as? String)?.contains("beta", ignoreCase = true)
            ?: false
        buildConfigField("Boolean", "IS_BETA", "$isBeta")

        val commitHash = (project.findProperty("app.commitHash") as? String) ?: ""
        buildConfigField("String", "GIT_COMMIT_HASH", "\"$commitHash\"")

        val appLabel = (project.findProperty("app.appName") as? String)
            ?: if (isBeta) "Vittify Beta" else "@string/app_name"
        manifestPlaceholders["appName"] = appLabel

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
            // Support CI/CD project properties or environment variables
            val storeFilePath = (project.findProperty("android.injected.signing.store.file") as? String)
                ?: System.getenv("RELEASE_STORE_FILE")
            val storePass = (project.findProperty("android.injected.signing.store.password") as? String)
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keyAliasProp = (project.findProperty("android.injected.signing.key.alias") as? String)
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPass = (project.findProperty("android.injected.signing.key.password") as? String)
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (!storeFilePath.isNullOrEmpty() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAliasProp
                keyPassword = keyPass
            } else {
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
            manifestPlaceholders["appName"] = "Vittify Beta"
            resValue("string", "app_package_name", "com.reddy.vittify.debug")
            buildConfigField("Boolean", "IS_BETA", "true")

            val optimizeDebug = (project.findProperty("app.optimizeDebug") as? String)?.toBoolean()
                ?: (project.findProperty("app.isBeta") as? String)?.toBoolean()
                ?: false

            if (optimizeDebug) {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("release")
            } else {
                versionNameSuffix = "-debug"
            }
        }
        release {
            val isBeta = (project.findProperty("app.isBeta") as? String)?.toBoolean() ?: false
            if (isBeta) {
                applicationIdSuffix = ".debug"
                manifestPlaceholders["appName"] = "Vittify Beta"
                resValue("string", "app_package_name", "com.reddy.vittify.debug")
            } else {
                manifestPlaceholders["appName"] = "@string/app_name"
                resValue("string", "app_package_name", "com.reddy.vittify")
            }
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
    implementation(libs.play.services.mlkit.document.scanner)
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
