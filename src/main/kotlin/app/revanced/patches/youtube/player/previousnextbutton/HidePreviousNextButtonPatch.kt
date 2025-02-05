package app.revanced.patches.youtube.player.previousnextbutton

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc

@Patch(
    name = "Hide previous next button",
    description = "Hides the previous and next button in the player controller.",
    dependencies = [SettingsPatch::class],
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
object HidePreviousNextButtonPatch : BytecodePatch(
    setOf(PlayerControlsVisibilityModelFingerprint)
) {
    private const val HAS_NEXT = 5
    private const val HAS_PREVIOUS = 6

    private const val INTEGRATIONS_METHOD_REFERENCE =
        "$PLAYER->hidePreviousNextButton(Z)Z"
    override fun execute(context: BytecodeContext) {

        PlayerControlsVisibilityModelFingerprint.result?.let {
            it.mutableMethod.apply {
                val callIndex = it.scanResult.patternScanResult!!.endIndex
                val callInstruction = getInstruction<Instruction3rc>(callIndex)

                val hasNextParameterRegister = callInstruction.startRegister + HAS_NEXT
                val hasPreviousParameterRegister = callInstruction.startRegister + HAS_PREVIOUS

                addInstructions(
                    callIndex, """
                        invoke-static { v$hasNextParameterRegister }, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v$hasNextParameterRegister
                        invoke-static { v$hasPreviousParameterRegister }, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v$hasPreviousParameterRegister
                        """
                )
            }
        } ?: throw PlayerControlsVisibilityModelFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PREVIOUS_NEXT_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide previous next button")

    }
}
