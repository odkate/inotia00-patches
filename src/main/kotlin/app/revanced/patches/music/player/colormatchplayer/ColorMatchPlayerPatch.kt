package app.revanced.patches.music.player.colormatchplayer

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.player.colormatchplayer.fingerprints.NewPlayerColorFingerprint
import app.revanced.patches.music.utils.fingerprints.PlayerColorFingerprint
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import kotlin.properties.Delegates

@Patch(
    name = "Enable color match player",
    description = "Matches the color of the mini player and the fullscreen player.",
    dependencies = [SettingsPatch::class],
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
object ColorMatchPlayerPatch : BytecodePatch(
    setOf(PlayerColorFingerprint)
) {
    private lateinit var miniPlayerReference1: Reference
    private lateinit var miniPlayerReference2: Reference
    private lateinit var miniPlayerReference3: Reference
    private lateinit var miniPlayerReference4: Reference
    private lateinit var miniPlayerReference5: Reference
    private lateinit var miniPlayerReference6: Reference

    private lateinit var miniPlayerIntReference1: Reference
    private lateinit var miniPlayerIntReference2: Reference
    private lateinit var miniPlayerIntReference3: Reference

    private var relativeIndex by Delegates.notNull<Int>()

    private fun MutableMethod.descriptor(index: Int): Reference {
        return getInstruction<ReferenceInstruction>(relativeIndex + index).reference
    }

    private fun MutableMethod.opcodeIndex(opcode: Opcode): Int {
        return implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == opcode
        }
    }

    override fun execute(context: BytecodeContext) {

        PlayerColorFingerprint.result?.let { parentResult ->
            parentResult.mutableMethod.apply {
                relativeIndex = parentResult.scanResult.patternScanResult!!.startIndex

                miniPlayerReference1 = descriptor(2)
                miniPlayerReference2 = descriptor(3)
                miniPlayerReference3 = descriptor(4)
                miniPlayerReference4 = descriptor(7)
                miniPlayerReference5 = descriptor(8)
                miniPlayerReference6 = descriptor(9)

                relativeIndex = opcodeIndex(Opcode.IGET_OBJECT) - 4

                miniPlayerIntReference1 = descriptor(0)
                miniPlayerIntReference2 = descriptor(3)

                relativeIndex = opcodeIndex(Opcode.IF_GEZ)
                miniPlayerIntReference3 = descriptor(1)

                val insertIndex = opcodeIndex(Opcode.IPUT_OBJECT)
                val jumpInstruction = getInstruction<Instruction>(insertIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex - 1).reference

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $MUSIC_PLAYER->enableColorMatchPlayer()Z
                        move-result v2
                        if-eqz v2, :off
                        iget v0, p0, $miniPlayerReference1
                        if-eq v0, v2, :switch
                        iput v2, p0, $miniPlayerReference1
                        iget-object v0, p0, $miniPlayerReference2
                        invoke-virtual {v0, v2, p2, p3}, $miniPlayerReference3
                        :switch
                        iget v0, p0, $miniPlayerReference4
                        if-eq v0, v1, :exit
                        iput v1, p0, $miniPlayerReference4
                        iget-object v0, p0, $miniPlayerReference5
                        invoke-virtual {v0, v1, p2, p3}, $miniPlayerReference6
                        goto :exit
                        :off
                        invoke-direct {p0}, $replaceReference
                        """, ExternalLabel("exit", jumpInstruction)
                )
                removeInstruction(insertIndex - 1)
            }

            NewPlayerColorFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val replaceReference =
                        getInstruction<ReferenceInstruction>(insertIndex - 1).reference

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {}, $MUSIC_PLAYER->enableColorMatchPlayer()Z
                            move-result v1
                            if-eqz v1, :off
                            iget v0, p0, $miniPlayerReference1
                            if-eq v0, v1, :switch
                            iput v1, p0, $miniPlayerReference1
                            iget-object v0, p0, $miniPlayerReference2
                            invoke-virtual {v0, v1, p2, p3}, $miniPlayerReference3
                            :switch
                            invoke-virtual {p1}, $miniPlayerIntReference1
                            move-result-object v1
                            check-cast v1, ${(miniPlayerIntReference2 as FieldReference).definingClass}
                            iget v1, v1, $miniPlayerIntReference2
                            invoke-static {v1}, $miniPlayerIntReference3
                            move-result v1
                            iget v0, p0, $miniPlayerReference4
                            if-eq v0, v1, :exit
                            iput v1, p0, $miniPlayerReference4
                            iget-object v0, p0, $miniPlayerReference5
                            invoke-virtual {v0, v1, p2, p3}, $miniPlayerReference6
                            :exit
                            return-void
                            :off
                            invoke-direct {p0}, $replaceReference
                            """
                    )
                    removeInstruction(insertIndex - 1)
                }
            } ?: throw NewPlayerColorFingerprint.exception
        } ?: throw PlayerColorFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_color_match_player",
            "true"
        )

    }
}