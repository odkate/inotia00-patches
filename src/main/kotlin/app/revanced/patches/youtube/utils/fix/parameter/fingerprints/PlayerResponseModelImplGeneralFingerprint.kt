package app.revanced.patches.youtube.utils.fix.parameter.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object PlayerResponseModelImplGeneralFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.RETURN_OBJECT,
        Opcode.CONST_4,
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/PlayerResponseModelImpl;") && methodDef.isWideLiteralExists(
            55735497
        )
    }
)
