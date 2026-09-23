import java.io.File

allprojects {
    // Repositories are declared in settings.gradle.kts (dependencyResolutionManagement).
}

// Генерирует assets/maps/maps.json — упорядоченный список уровней (миры → карты → final).
// Нужен потому, что в jar/APK содержимое каталогов не перечислить (FileHandle.list()).
val generateMapsManifest = tasks.register("generateMapsManifest") {
    val mapsDir = file("assets/maps")
    val manifest = mapsDir.resolve("maps.json")
    inputs.files(
        fileTree(mapsDir) {
            include("**/*.txt")
            include("**/theme.json")
            include("final.json")
        }
    ).withPropertyName("mapsSources")
    outputs.file(manifest).withPropertyName("mapsManifest")

    doLast {
        fun themeOf(file: File): String {
            if (!file.exists()) return "system"
            val match = Regex("\"theme\"\\s*:\\s*\"([^\"]+)\"").find(file.readText())
            return match?.groupValues?.get(1) ?: "system"
        }

        // Сохраняем флаг use из предыдущего манифеста, чтобы ручная разметка уровней не терялась при перегенерации.
        // По умолчанию (нет предыдущего значения) уровень включается в сборку: use = true.
        val prevText = if (manifest.exists()) manifest.readText() else ""
        fun prevUse(path: String): Boolean {
            val m = Regex("\"path\"\\s*:\\s*\"$path\"[^}]*?\"use\"\\s*:\\s*(true|false)").find(prevText)
            return m?.groupValues?.get(1) != "false"
        }

        val worlds = (mapsDir.listFiles { f: File -> f.isDirectory && f.name.toIntOrNull() != null } ?: emptyArray())
            .sortedBy { it.name.toInt() }
        val levels = mutableListOf<String>()
        for (world in worlds) {
            val theme = themeOf(File(world, "theme.json"))
            val cards = (world.listFiles { f: File -> f.isFile && f.name.endsWith(".txt") } ?: emptyArray())
                .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            for (card in cards) {
                val path = "maps/${world.name}/${card.name}"
                levels.add("    { \"path\": \"$path\", \"theme\": \"$theme\", \"final\": false, \"use\": ${prevUse(path)} }")
            }
        }

        val finalTxt = File(mapsDir, "final.txt")
        if (finalTxt.exists()) {
            val theme = themeOf(File(mapsDir, "final.json"))
            levels.add("    { \"path\": \"maps/final.txt\", \"theme\": \"$theme\", \"final\": true, \"use\": ${prevUse("maps/final.txt")} }")
        }

        val json = "{\n  \"levels\": [\n" + levels.joinToString(",\n") + "\n  ]\n}\n"
        manifest.writeText(json)
        logger.lifecycle("generateMapsManifest: ${levels.size} levels -> ${manifest.relativeTo(rootDir)}")
    }
}

subprojects {
    afterEvaluate {
        tasks.matching { it.name == "processResources" }.configureEach {
            dependsOn(generateMapsManifest)
        }
        tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
            dependsOn(generateMapsManifest)
        }
    }
}
