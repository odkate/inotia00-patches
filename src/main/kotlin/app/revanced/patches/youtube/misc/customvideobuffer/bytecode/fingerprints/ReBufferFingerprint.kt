package app.revanced.patches.youtube.misc.customvideobuffer.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.Opcode

object ReBufferFingerprint : MethodFingerprint(
    "I", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf(),
    listOf(Opcode.IF_LEZ, Opcode.RETURN),
    customFingerprint = { methodDef ->
        methodDef.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;"
                && methodDef.implementation!!.instructions.any {
            ((it as? NarrowLiteralInstruction)?.narrowLiteral == 5000)
        }
    }
)