package codes.dreaming.morphe.patches.instagram.picker

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import codes.dreaming.morphe.patches.instagram.Constants.COMPATIBILITY_INSTAGRAM
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS =
    "Lcodes/dreaming/morphe/extension/instagram/SystemPhotoPickerPatch;"

@Suppress("unused")
val systemPhotoPickerPatch = bytecodePatch(
    name = "Use system photo picker",
    description = "Adds an entry to the post and DM gallery album lists that opens the Android system photo picker. " +
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

        // The album picker draws the entry with Instagram's Google Photos label and
        // logo. Replace both with the photo picker label and a neutral icon.
        AlbumPickerEntryListFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val idIndex = fingerprint.instructionMatches[0].index
            val createIndex = (idIndex + 1 until method.instructions.size).firstOrNull {
                method.getInstruction(it).opcode == Opcode.NEW_INSTANCE
            } ?: throw PatchException("No album picker entry created after the id ${ALBUM_PICKER_EXTERNAL_ENTRY_ID}")
            val albumEntryType =
                (method.getInstruction<ReferenceInstruction>(createIndex).reference as TypeReference).type

            fun fieldPutIndex(fieldType: String, definingClass: String? = null) =
                (createIndex + 1 until method.instructions.size).firstOrNull { index ->
                    val instruction = method.getInstruction(index)
                    if (instruction.opcode != Opcode.IPUT_OBJECT) return@firstOrNull false
                    val field = (instruction as ReferenceInstruction).reference as FieldReference
                    field.type == fieldType && (definingClass == null || field.definingClass == definingClass)
                } ?: throw PatchException("$albumEntryType has no $fieldType store after it is created")

            val labelIndex = fieldPutIndex("Ljava/lang/String;", albumEntryType)
            val iconIndex = fieldPutIndex("Ljava/lang/Integer;")

            // Insert at the later index first so the earlier index stays valid.
            listOf(labelIndex, iconIndex).sortedDescending().forEach { index ->
                val register = method.getInstruction<OneRegisterInstruction>(index).registerA
                val call = if (index == labelIndex) {
                    "invoke-static { }, $EXTENSION_CLASS->getPickerLabel()Ljava/lang/String;"
                } else {
                    "invoke-static/range { v$register .. v$register }, " +
                        "$EXTENSION_CLASS->getPickerIconRes(Ljava/lang/Integer;)Ljava/lang/Integer;"
                }
                method.addInstructions(
                    index,
                    """
                        $call
                        move-result-object v$register
                    """,
                )
            }
        }

        // The album menu of the stories and reels gallery takes the label from the
        // factory entry but draws the Google Photos logo. Replace the logo.
        val albumMenuCall = AlbumMenuGooglePhotosFlagFingerprint.matchAllOrNull().orEmpty().firstOrNull()
            ?.let { it.instructionMatches[1].getInstruction<ReferenceInstruction>().reference as MethodReference }
            ?: throw PatchException("No album menu call found")
        val albumMenuIcon = mutableClassDefBy(albumMenuCall.definingClass).methods.firstNotNullOfOrNull { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@firstNotNullOfOrNull null
            val factoryCall = instructions.indexOfFirst {
                ((it as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                    ref.definingClass == factory.definingClass && ref.name == factory.name
                } == true
            }
            if (factoryCall < 0) return@firstNotNullOfOrNull null
            val drawableCall = (factoryCall + 1 until instructions.size).firstOrNull {
                val ref = (instructions[it] as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Landroid/content/Context;" && ref.name == "getDrawable"
            } ?: return@firstNotNullOfOrNull null
            method to drawableCall + 1
        } ?: throw PatchException("${albumMenuCall.definingClass} has no Context.getDrawable call after the factory call")
        albumMenuIcon.let { (method, resultIndex) ->
            if (method.getInstruction(resultIndex).opcode != Opcode.MOVE_RESULT_OBJECT) {
                throw PatchException("Context.getDrawable result is not stored")
            }
            val register = method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
            method.addInstructions(
                resultIndex + 1,
                """
                    invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->getPickerIconDrawable(Landroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;
                    move-result-object v$register
                """,
            )
        }

        // Every album picker host passes the entry gate as the last argument of the
        // album picker config. The post gallery host and the DM album picker take
        // the entry from there, so force it on in the constructor.
        val configInit = AlbumPickerConfigFingerprint.instructionMatches[1]
            .getInstruction<ReferenceInstruction>().reference as MethodReference
        val configConstructor = mutableClassDefBy(configInit.definingClass).methods.single {
            it.name == "<init>" &&
                it.parameterTypes.map(CharSequence::toString) == configInit.parameterTypes.map(CharSequence::toString)
        }
        configConstructor.addInstruction(0, "const/16 p6, 0x1")

        // The DM media picker also gates its album menu callback behind a
        // MobileConfig flag. Force every check of it.
        val dmFlagChecks = MobileConfigFlagFingerprint(DM_GOOGLE_PHOTOS_ENTRY_FLAG).matchAllOrNull().orEmpty()
            .sumOf { forceMobileConfigFlag(it.method, DM_GOOGLE_PHOTOS_ENTRY_FLAG) }
        if (dmFlagChecks == 0) throw PatchException("No check of the DM gallery flag found")

        // The stories and reels gallery and the full-screen creation gallery pass
        // the entry callback to the album menu only if a MobileConfig flag is on.
        // Both galleries launch the entry with request 4921 and import the result.
        val creationFlagChecks = MobileConfigFlagFingerprint(GOOGLE_PHOTOS_ENTRY_FLAG).matchAllOrNull().orEmpty()
            .sumOf { forceMobileConfigFlag(it.method, GOOGLE_PHOTOS_ENTRY_FLAG) }
        if (creationFlagChecks == 0) throw PatchException("No check of the creation gallery flag found")
    }
}

/**
 * Sets the result of every check of [flag] in [method] to true and returns the
 * number of checks. A check loads the flag id and reads a boolean right after it.
 * Other uses of the literal are left alone.
 */
private fun forceMobileConfigFlag(method: MutableMethod, flag: Long): Int {
    val literalIndices = method.instructions.withIndex()
        .filter { (_, instruction) -> (instruction as? WideLiteralInstruction)?.wideLiteral == flag }
        .map { it.index }
    // Patch from the end so earlier indices stay valid.
    return literalIndices.sortedDescending().count { literalIndex ->
        val resultIndex = (literalIndex + 1..minOf(literalIndex + 4, method.instructions.size - 1)).firstOrNull {
            method.getInstruction(it).opcode == Opcode.MOVE_RESULT
        } ?: return@count false
        val register = method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
        method.addInstruction(resultIndex + 1, "const/16 v$register, 0x1")
        true
    }
}
