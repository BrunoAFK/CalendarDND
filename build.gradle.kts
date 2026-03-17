// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("com.google.firebase.firebase-perf") version "2.0.0" apply false
}

buildscript {
    dependencies {
        // AGP pulls Netty through its UTP/gRPC toolchain on the build classpath.
        // Pin the build-time Netty stack to a patched release to satisfy Dependabot.
        classpath(enforcedPlatform("io.netty:netty-bom:4.1.129.Final"))
        // UTP / Google testing platform also brings Protobuf on the build classpath.
        // Keep the whole Protobuf family at a patched version for dependency scanning.
        classpath(enforcedPlatform("com.google.protobuf:protobuf-bom:3.25.5"))
        // AGP's Jetifier stack still brings JDOM on the build classpath.
        classpath("org.jdom:jdom2:2.0.6.1")
        // Bundletool in the AGP toolchain still resolves jose4j transitively.
        classpath("org.bitbucket.b_c:jose4j:0.9.6")
        // AGP toolchain still resolves commons-compress transitively.
        classpath("org.apache.commons:commons-compress:1.26.0")
    }
}
