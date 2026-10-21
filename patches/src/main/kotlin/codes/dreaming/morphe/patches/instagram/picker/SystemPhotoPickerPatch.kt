package codes.dreaming.morphe.patches.instagram.picker

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import codes.dreaming.morphe.patches.instagram.Constants.COMPATIBILITY_INSTAGRAM
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lcodes/dreaming/morphe/extension/instagram/SystemPhotoPickerPatch;"

@Suppress("unused")
val systemPhotoPickerPatch = bytecodePatch(
    name = "Use system photo picker",
    description = "Adds an entry to the DM gallery album list that opens the Android system photo picker. " +
        "The picker also shows media from cloud media providers such as Immich.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)

    extendWith("extensions/instagram.mpe")

    execute {
        val factory = ExternalGalleryEntryFingerprint.method
        val entryType = factory.returnType
        val entryClass = classDefBy(entryType)

        fun fieldOf(type: String) = entryClass.fields.singleOrNull { it.type == type }
            ?: throw PatchException("$entryType has no single field of type $type")

        val intentField = fieldOf("Landroid/content/Intent;")
        val iconField = fieldOf("Landroid/graphics/drawable/Drawable;")
        val labelField = fieldOf("Ljava/lang/String;")

        // The entry class has no constructor of its own. Reuse the constructor
        // call that the original code makes after it creates the entry.
        val entryInit = factory.instructions.zipWithNext().firstNotNullOfOrNull { (create, init) ->
            if (create.opcode != Opcode.NEW_INSTANCE) return@firstNotNullOfOrNull null
            if ((create as ReferenceInstruction).reference.toString() != entryType) return@firstNotNullOfOrNull null
            ((init as? ReferenceInstruction)?.reference as? MethodReference)
                ?.takeIf { it.name == "<init>" && it.parameterTypes.isEmpty() }
        } ?: throw PatchException("${factory.name} does not create $entryType with a no-argument constructor")

        val localRegisters = factory.implementation!!.registerCount - 1
        if (localRegisters < 3) {
            throw PatchException("${factory.name} has $localRegisters local registers, the hook needs 3")
        }

        factory.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0 }, $EXTENSION_CLASS->getPickerIntent(Landroid/content/pm/PackageManager;)Landroid/content/Intent;
                move-result-object v0
                if-eqz v0, :original
                new-instance v1, $entryType
                invoke-direct { v1 }, ${entryInit.definingClass}-><init>()V
                iput-object v0, v1, $entryType->${intentField.name}:Landroid/content/Intent;
                invoke-static { p0, v0 }, $EXTENSION_CLASS->getPickerIcon(Landroid/content/pm/PackageManager;Landroid/content/Intent;)Landroid/graphics/drawable/Drawable;
                move-result-object v2
                iput-object v2, v1, $entryType->${iconField.name}:Landroid/graphics/drawable/Drawable;
                invoke-static { }, $EXTENSION_CLASS->getPickerLabel()Ljava/lang/String;
                move-result-object v2
                iput-object v2, v1, $entryType->${labelField.name}:Ljava/lang/String;
                return-object v1
            """,
            ExternalLabel("original", factory.getInstruction(0)),
        )

        // Instagram hides the entry behind a MobileConfig flag. The flag reaches
        // the album picker config as the last constructor argument.
        val configInit = AlbumPickerConfigFingerprint.instructionMatches[1]
            .getInstruction<ReferenceInstruction>().reference as MethodReference
        val configConstructor = mutableClassDefBy(configInit.definingClass).methods.single {
            it.name == "<init>" &&
                it.parameterTypes.map(CharSequence::toString) == configInit.parameterTypes.map(CharSequence::toString)
        }
        configConstructor.addInstruction(0, "const/16 p6, 0x1")
    }
}
