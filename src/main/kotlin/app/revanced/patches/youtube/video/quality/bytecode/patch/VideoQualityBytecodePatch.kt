package app.revanced.patches.youtube.video.quality.bytecode.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.VideoQualityReferenceFingerprint
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.VideoQualitySetterFingerprint
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.VideoUserQualityChangeFingerprint
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Name("default-video-quality-bytecode-patch")
@DependsOn([LegacyVideoIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class VideoQualityBytecodePatch : BytecodePatch(
    listOf(
        VideoQualitySetterFingerprint
    )
) {
    private companion object {
        const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoQualityPatch;"
    }
    override fun execute(context: BytecodeContext): PatchResult {
        val setterMethod = VideoQualitySetterFingerprint.result!!

        VideoUserQualityChangeFingerprint.resolve(context, setterMethod.classDef)
        val userQualityResult = VideoUserQualityChangeFingerprint.result!!

        VideoQualityReferenceFingerprint.resolve(context, setterMethod.classDef)
        val qualityFieldReference =
            VideoQualityReferenceFingerprint.result!!.method.let { method ->
                (method.implementation!!.instructions.elementAt(0) as ReferenceInstruction).reference as FieldReference
            }

        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        val qIndexMethodName =
            context.classes.single { it.type == qualityFieldReference.type }.methods.single { it.parameterTypes.first() == "I" }.name

        setterMethod.mutableMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${setterMethod.classDef.type}->${qualityFieldReference.name}:${qualityFieldReference.type}
                const-string v1, "$qIndexMethodName"
                invoke-static {p1, p2, v0, v1}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/String;)I
                   move-result p2
            """,
        )

        userQualityResult.mutableMethod.addInstruction(
            0,
            "invoke-static {p3}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
        )

        return PatchResultSuccess()
    }
}