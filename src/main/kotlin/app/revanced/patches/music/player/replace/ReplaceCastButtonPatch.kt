package app.revanced.patches.music.player.replace

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.replace.fingerprints.CastButtonContainerFingerprint
import app.revanced.patches.music.utils.playerresponse.PlayerResponsePatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerCastMediaRouteButton
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.settings.SettingsPatch.contexts
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Replace cast button",
    description = "Replace the cast button in the player with the open music button.",
    dependencies = [
        PlayerResponsePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class
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
    ],
    use = false
)
@Suppress("unused")
object ReplaceCastButtonPatch : BytecodePatch(
    setOf(CastButtonContainerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CastButtonContainerFingerprint.result?.let {
            it.mutableMethod.apply {
                val freeIndex = getWideLiteralIndex(PlayerCastMediaRouteButton) + 1
                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                val getActivityIndex = freeIndex - 4
                val getActivityRegister =
                    getInstruction<TwoRegisterInstruction>(getActivityIndex).registerB
                val getActivityReference =
                    getInstruction<ReferenceInstruction>(getActivityIndex).reference

                for (index in freeIndex + 20 downTo freeIndex) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((getInstruction<ReferenceInstruction>(index).reference as MethodReference).name != "addView")
                        continue

                    val viewGroupInstruction = getInstruction<Instruction35c>(index)

                    addInstruction(
                        index + 1,
                        "invoke-static {v$freeRegister, v${viewGroupInstruction.registerC}, v${viewGroupInstruction.registerD}}, " +
                                MUSIC_PLAYER +
                                "->" +
                                "replaceCastButton(Landroid/app/Activity;Landroid/view/ViewGroup;Landroid/view/View;)V"
                    )
                    addInstruction(
                        index + 1,
                        "iget-object v$freeRegister, v$getActivityRegister, $getActivityReference"
                    )
                    removeInstruction(index)

                    break
                }
            }
        } ?: throw CastButtonContainerFingerprint.exception

        PlayerResponsePatch.injectPlaylistCall(
            "$MUSIC_UTILS_PATH/CheckMusicVideoPatch;" +
                    "->" +
                    "playbackStart(Ljava/lang/String;Ljava/lang/String;IZ)V"
        )

        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "open_music_button.xml"
            )
        ).forEach { resourceGroup ->
            contexts.copyResources("music/cast", resourceGroup)
        }

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_replace_player_cast_button",
            "false"
        )

    }
}
