group = "codes.dreaming"

patches {
    about {
        name = "Dreaming-Codes Patches"
        description = "Morphe patches by Dreaming-Codes"
        source = "git@github.com:Dreaming-Codes/morphe-patches.git"
        author = "Dreaming-Codes"
        contact = "https://github.com/Dreaming-Codes/morphe-patches/issues"
        website = "https://github.com/Dreaming-Codes/morphe-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
