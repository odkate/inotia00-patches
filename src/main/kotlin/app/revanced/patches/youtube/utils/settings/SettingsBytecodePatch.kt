package app.revanced.patches.youtube.utils.settings

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH

@Patch(
    dependencies = [
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class
    ]
)
object SettingsBytecodePatch : BytecodePatch(
    setOf(ThemeSetterSystemFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        contexts = context

        // apply the current theme of the settings page
        ThemeSetterSystemFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                replaceInstruction(
                    targetIndex,
                    SET_THEME
                )
                addInstruction(
                    targetIndex + 1,
                    "return-object v0"
                )
                addInstruction(
                    this.implementation!!.instructions.size - 1,
                    SET_THEME
                )
            }
        } ?: throw ThemeSetterSystemFingerprint.exception

        context.injectInit("InitializationPatch", "setDeviceInformation", true)
        context.injectInit("InitializationPatch", "initializeReVancedSettings", true)

    }
    internal lateinit var contexts: BytecodeContext

    private const val SET_THEME =
        "invoke-static {v0}, $INTEGRATIONS_PATH/utils/ThemeHelper;->setTheme(Ljava/lang/Object;)V"
}
