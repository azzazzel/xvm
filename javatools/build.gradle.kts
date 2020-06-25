/*
 * Build file for the Java tools portion of the XDK.
 */

plugins {
    java
}

tasks.register<Copy>("copyImplicits") {
    description = "Copy the implicit.x from :ecstasy project into the build directory."
    from(file(project(":ecstasy").property("implicit.x")))
    into(file("$buildDir/resources/main/"))
}

tasks.register<Copy>("copyUtils") {
    description = "Copy the classes from :utils project into the build directory."
    from(file("${project(":utils").buildDir}/classes/java/main"))
    include("**/*.class")
    into(file("$buildDir/classes/java/main"))
}

tasks.withType(Jar::class) {
    val copyImplicits = tasks["copyImplicits"]
    val copyUtils     = tasks["copyUtils"]

    dependsOn(copyImplicits)
    dependsOn(copyUtils)

    mustRunAfter(copyImplicits)
    mustRunAfter(copyUtils)

    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Sealed"] = "true"
        attributes["Main-Class"] = "org.xvm.tool.Launcher"
        attributes["Name"] = "/org/xvm/"
        attributes["Specification-Title"] = "xvm"
        attributes["Specification-Version"] = "0.1.0"
        attributes["Specification-Vendor"] = "xtclang.org"
        attributes["Implementation-Title"] = "xvm-prototype"
        attributes["Implementation-Version"] = "0.1.0"
        attributes["Implementation-Vendor"] = "xtclang.org"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.xtclang.xvm:utils:")

    // Use JUnit test framework
    testImplementation("junit:junit:4.12")
}
