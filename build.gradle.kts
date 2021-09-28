import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    java
    kotlin("jvm") version "1.4.32"
}

group = "com.xx.qqbot"
version = "0.0.1"

repositories {
    mavenCentral()
}

tasks.withType(KotlinJvmCompile::class.java) {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    // 开发时使用 mirai-core-api，运行时提供 mirai-core

//    api("net.mamoe:mirai-core-api:${properties["version.mirai"]}")
//    runtimeOnly("net.mamoe:mirai-core:${properties["version.mirai"]}")

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.squareup.okhttp3:okhttp:4.2.2")

    api("net.mamoe:mirai-core:2.8.0-M1")
    // 可以简单地只添加 api("net.mamoe:mirai-core:2.6.1")
}