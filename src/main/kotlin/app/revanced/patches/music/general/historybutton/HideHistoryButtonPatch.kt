package app.revanced.patches.music.general.historybutton

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.general.historybutton.fingerprints.HistoryMenuItemFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Hide history button",
    description = "Hides history button in toolbar.",
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
object HideHistoryButtonPatch : BytecodePatch(
    setOf(HistoryMenuItemFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        HistoryMenuItemFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $MUSIC_GENERAL->hideHistoryButton(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        } ?: throw HistoryMenuItemFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_history_button",
            "false"
        )

    }
}
