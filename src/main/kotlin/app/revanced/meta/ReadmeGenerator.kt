package app.revanced.meta

import app.revanced.patcher.PatchSet
import app.revanced.patcher.patch.Patch
import java.io.File

internal class ReadmeGenerator : PatchesFileGenerator {
    private companion object {
        private const val TABLE_HEADER =
            "| \uD83D\uDC8A Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                    "|:--------:|:--------------:|:-----------------:|"
    }

    override fun generate(patches: PatchSet) {
        val output = StringBuilder()

        mutableMapOf<String, MutableSet<Patch<*>>>()
            .apply {
                for (patch in patches) {
                    patch.compatiblePackages?.forEach { pkg ->
                        if (!contains(pkg.name)) put(pkg.name, mutableSetOf())
                        this[pkg.name]!!.add(patch)
                    }
                }
            }
            .entries
            .sortedByDescending { it.value.size }
            .forEach { (`package`, patches) ->
                output.apply {
                    appendLine("### [\uD83D\uDCE6 `${`package`}`](https://play.google.com/store/apps/details?id=${`package`})")
                    appendLine("<details>\n")
                    appendLine(TABLE_HEADER)
                    patches.sortedBy { it.name }.forEach { patch ->
                        val supportedVersionArray =
                            patch.compatiblePackages?.single { it.name == `package` }?.versions
                        val supportedVersion =
                            if (supportedVersionArray?.isNotEmpty() == true) {
                                val minVersion = supportedVersionArray.elementAt(0)
                                val maxVersion =
                                    supportedVersionArray.elementAt(supportedVersionArray.size - 1)
                                if (minVersion == maxVersion)
                                    maxVersion
                                else
                                    "$minVersion ~ $maxVersion"
                            } else
                                "all"

                        appendLine(
                            "| `${patch.name}` " +
                                    "| ${patch.description} " +
                                    "| $supportedVersion |"
                        )
                    }
                    appendLine("</details>\n")
                }
            }

        StringBuilder(File("README-template.md").readText())
            .replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
            .let(File("README.md")::writeText)
    }
}