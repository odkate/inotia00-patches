package app.revanced.patches.music.navigation.component

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.navigation.component.fingerprints.TabLayoutTextFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_NAVIGATION
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Hide navigation bar component",
    description = "Hides navigation bar components.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.20.51",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object NavigationBarComponentPatch : BytecodePatch(
    setOf(TabLayoutTextFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        /**
         * Hide navigation labels
         */
        TabLayoutTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(SharedResourceIdPatch.Text1) + 3
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $MUSIC_NAVIGATION->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        } ?: throw TabLayoutTextFingerprint.exception

        SettingsPatch.contexts.xmlEditor[RESOURCE_FILE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }

        /**
         * Hide navigation bar & buttons
         */
        TabLayoutTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val enumIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.AND_INT_LIT8
                } - 2

                val pivotTabIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "getVisibility"
                }
                val pivotTabRegister = getInstruction<Instruction35c>(pivotTabIndex).registerC

                addInstruction(
                    pivotTabIndex,
                    "invoke-static {v$pivotTabRegister}, $MUSIC_NAVIGATION->hideNavigationButton(Landroid/view/View;)V"
                )

                addInstruction(
                    insertIndex,
                    "sput-object v$enumRegister, $MUSIC_NAVIGATION->lastPivotTab:Ljava/lang/Enum;"
                )
            }
        } ?: throw TabLayoutTextFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_explore_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_home_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_library_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_bar",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_label",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_samples_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_upgrade_button",
            "true"
        )
    }

    private const val FLAG = "android:layout_weight"
    private const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"
}
