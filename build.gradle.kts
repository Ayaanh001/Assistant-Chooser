buildscript {
    val agp_version by extra("8.5.2")
    val agp_version1 by extra("8.6.0")
}
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
