package app.revanced.patches.youtube.utils.sponsorblock

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch(
    name = "SponsorBlock",
    description = "Integrates SponsorBlock which allows skipping video segments such as sponsored content.",
    dependencies = [
        SettingsPatch::class,
        SponsorBlockBytecodePatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.41"
            ]
        )
    ]
)
@Suppress("unused")
object SponsorBlockPatch : ResourcePatch() {
    private val OutlineIcon by booleanPatchOption(
        key = "OutlineIcon",
        default = false,
        title = "Outline icons",
        description = "Apply the outline icon",
        required = true
    )

    override fun execute(context: ResourceContext) {
        /**
         * merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "inline_sponsor_overlay.xml",
                "skip_sponsor_button.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/sponsorblock/shared", resourceGroup)
        }

        if (OutlineIcon == true) {
            arrayOf(
                ResourceUtils.ResourceGroup(
                    "layout",
                    "new_segment.xml"
                ),
                ResourceUtils.ResourceGroup(
                    "drawable",
                    "ic_sb_adjust.xml",
                    "ic_sb_backward.xml",
                    "ic_sb_compare.xml",
                    "ic_sb_edit.xml",
                    "ic_sb_forward.xml",
                    "ic_sb_logo.xml",
                    "ic_sb_publish.xml",
                    "ic_sb_voting.xml",
                    "ns_bg.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/sponsorblock/outline", resourceGroup)
            }
        } else {
            arrayOf(
                ResourceUtils.ResourceGroup(
                    "layout",
                    "new_segment.xml"
                ),
                ResourceUtils.ResourceGroup(
                    "drawable",
                    "ic_sb_adjust.xml",
                    "ic_sb_compare.xml",
                    "ic_sb_edit.xml",
                    "ic_sb_logo.xml",
                    "ic_sb_publish.xml",
                    "ic_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/sponsorblock/default", resourceGroup)
            }
        }

        /**
         * merge xml nodes from the host to their real xml files
         */
        // collect all host resources
        val hostingXmlResources = mapOf("layout" to arrayOf("youtube_controls_layout"))

        // copy nodes from host resources to their real xml files
        hostingXmlResources.forEach { (path, resources) ->
            resources.forEach { resource ->
                val hostingResourceStream =
                    this.javaClass.classLoader.getResourceAsStream("youtube/sponsorblock/shared/host/$path/$resource.xml")!!

                val targetXmlEditor = context.xmlEditor["res/$path/$resource.xml"]
                "RelativeLayout".copyXmlNode(
                    context.xmlEditor[hostingResourceStream],
                    targetXmlEditor
                ).also {
                    val children = targetXmlEditor.file.getElementsByTagName("RelativeLayout")
                        .item(0).childNodes

                    // Replace the startOf with the voting button view so that the button does not overlap
                    for (i in 1 until children.length) {
                        val view = children.item(i)

                        // Replace the attribute for a specific node only
                        if (!(view.hasAttributes() && view.attributes.getNamedItem("android:id").nodeValue.endsWith(
                                "player_video_heading"
                            ))
                        ) continue

                        // voting button id from the voting button view from the youtube_controls_layout.xml host file
                        val votingButtonId = "@+id/sb_voting_button"

                        view.attributes.getNamedItem("android:layout_toStartOf").nodeValue =
                            votingButtonId

                        break
                    }
                }.close() // close afterwards
            }
        }

        /**
         * Add ReVanced Extended Settings
         */
        SettingsPatch.addReVancedPreference("sponsorblock_settings")

        SettingsPatch.updatePatchStatus("SponsorBlock")

    }
}
