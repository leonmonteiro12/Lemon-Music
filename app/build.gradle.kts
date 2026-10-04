import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application") version "8.11.0"
}

val keystorePropsFile =
    rootProject.file("release.properties")

val keystoreProps =
    Properties()

if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use {
        keystoreProps.load(it)
    }
}

val hasValidSigningProps =
    listOf(
        "storeFile",
        "storePassword",
        "keyAlias",
        "keyPassword"
    ).all {
        keystoreProps[it] != null
    }

android {

    namespace = "com.lemon.music"

    compileSdk = 36

    defaultConfig {

        applicationId = "com.lemon.music"

        minSdk = 23

        targetSdk = 36

        versionCode = 8

        versionName = "6.20.51"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }

    signingConfigs {

        if (hasValidSigningProps) {

            create("release") {

                storeFile =
                    rootProject.file(
                        keystoreProps["storeFile"] as String
                    )

                storePassword =
                    keystoreProps["storePassword"] as String

                keyAlias =
                    keystoreProps["keyAlias"] as String

                keyPassword =
                    keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {

        getByName("debug") {
            isMinifyEnabled = false
        }

        getByName("release") {

            if (hasValidSigningProps) {

                signingConfig =
                    signingConfigs.getByName("release")
            }

            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        checkReleaseBuilds = false
    }

    packaging {

        resources {

            excludes.add(
                "/META-INF/{AL2.0,LGPL2.1}"
            )

            excludes.add(
                "META-INF/kotlinx_coroutines_core.version"
            )

            excludes.add(
                "META-INF/kotlin-project-structure-metadata.json"
            )

            pickFirsts.add(
                "nonJvmMain/default/linkdata/package_androidx/0_androidx.knm"
            )

            pickFirsts.add(
                "nonJvmMain/default/linkdata/root_package/0_.knm"
            )

            pickFirsts.add(
                "nonJvmMain/default/linkdata/module"
            )

            pickFirsts.add(
                "nativeMain/default/linkdata/root_package/0_.knm"
            )

            pickFirsts.add(
                "nativeMain/default/linkdata/module"
            )

            pickFirsts.add(
                "commonMain/default/linkdata/root_package/0_.knm"
            )

            pickFirsts.add(
                "commonMain/default/linkdata/package_androidx/0_androidx.knm"
            )

            merges.add(
                "commonMain/default/manifest"
            )

            merges.add(
                "nonJvmMain/default/manifest"
            )

            merges.add(
                "nativeMain/default/manifest"
            )
        }
    }
}

configurations.all {

    resolutionStrategy {

        force(
            "org.jetbrains.kotlin:kotlin-stdlib:1.9.22"
        )

        force(
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22"
        )

        force(
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22"
        )

        force(
            "androidx.collection:collection:1.4.2"
        )

        force(
            "androidx.annotation:annotation:1.8.1"
        )

        force(
            "androidx.core:core-ktx:1.8.0"
        )

        force(
            "androidx.lifecycle:lifecycle-runtime-ktx:2.3.1"
        )

        force(
            "androidx.collection:collection-ktx:1.4.2"
        )
    }
}

dependencies {

    // =========================================================
    // ANDROIDX
    // =========================================================

    implementation(
        "androidx.appcompat:appcompat:1.7.1"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.1.4"
    )

    implementation(
        "androidx.recyclerview:recyclerview:1.4.0"
    )

    implementation(
        "androidx.startup:startup-runtime:1.1.1"
    )

    implementation(
        "androidx.interpolator:interpolator:1.0.0"
    )

    // =========================================================
    // MATERIAL
    // =========================================================

    implementation(
        "com.google.android.material:material:1.13.0"
    )

    // =========================================================
    // MEDIA
    // =========================================================

    implementation(
        "androidx.media:media:1.7.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer:1.11.1"
    )

    implementation(
        "androidx.media3:media3-session:1.11.1"
    )

    // =========================================================
    // GOOGLE AUTH
    // =========================================================

    implementation(
        "androidx.credentials:credentials:1.5.0"
    )

    implementation(
        "androidx.credentials:credentials-play-services-auth:1.5.0"
    )

    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )

    implementation(
        "com.google.android.gms:play-services-auth:21.4.0"
    )

    // =========================================================
    // INNER TUBE / INNERTUNE
    // =========================================================

    implementation(
        project(":innertube")
    )

    // =========================================================
    // JAVA API DESUGARING — ANDROID 6 NIO
    // =========================================================

    coreLibraryDesugaring(
        "com.android.tools:desugar_jdk_libs_nio:2.1.5"
    )
}