/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Fonts the user adds from a .ttf or .otf file. A chosen font is copied into the app's private storage
 * (`files/fonts`) so it keeps working when the original file moves. The island font setting stores the id
 * `custom:<file name>`; the helpers here are pure so they can be tested without a device.
 */
object CustomFonts {
    const val ID_PREFIX = "custom:"
    const val MAX_BYTES = 20L * 1024 * 1024
    private const val DIR = "fonts"
    private const val MAX_BASE_NAME = 60
    private const val FALLBACK_NAME = "Custom font"
    private val SAFE_FILE_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9._ \\-]{0,78}\\.(ttf|otf)$")

    /** The result of adding a font. */
    sealed interface ImportResult {
        data class Added(val id: String) : ImportResult
        data object TooLarge : ImportResult
        data object NotAFont : ImportResult
        data object Failed : ImportResult
    }

    fun isCustomId(id: String): Boolean = id.startsWith(ID_PREFIX)

    fun idFor(fileName: String): String = ID_PREFIX + fileName

    /** The stored file name behind a font id, or null when [id] is not a well-formed custom font id. */
    fun fileNameOf(id: String): String? {
        if (!id.startsWith(ID_PREFIX)) return null
        val name = id.substring(ID_PREFIX.length)
        return name.takeIf { SAFE_FILE_NAME.matches(it) && !it.contains("..") }
    }

    /** The name shown in the picker: the file name without its extension. */
    fun labelOf(fileName: String): String = fileName.substringBeforeLast('.')

    /** "ttf" or "otf" from the first bytes of a font file, or null when it is neither. */
    fun sniffExtension(head: ByteArray): String? {
        if (head.size < 4) return null
        val b0 = head[0].toInt() and 0xFF
        val b1 = head[1].toInt() and 0xFF
        val b2 = head[2].toInt() and 0xFF
        val b3 = head[3].toInt() and 0xFF
        return when {
            b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00 -> "ttf"
            b0 == 'O'.code && b1 == 'T'.code && b2 == 'T'.code && b3 == 'O'.code -> "otf"
            b0 == 't'.code && b1 == 'r'.code && b2 == 'u'.code && b3 == 'e'.code -> "ttf"
            else -> null
        }
    }

    /** A safe stored file name from the picked file's display name and its real format. */
    fun safeFileName(displayName: String?, extension: String): String {
        val base = (displayName ?: "")
            .substringBeforeLast('.', displayName ?: "")
            .replace(Regex("[^A-Za-z0-9._ \\-]"), "_")
            .replace("..", "_")
            .trim(' ', '.', '_', '-')
            .take(MAX_BASE_NAME)
            .trim(' ', '.', '_', '-')
            .ifEmpty { FALLBACK_NAME }
        return "$base.$extension"
    }

    /** [name] if it is free, otherwise "name (2).ext", "name (3).ext" ... */
    fun uniqueFileName(name: String, taken: Set<String>): String {
        if (name !in taken) return name
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.')
        var n = 2
        while ("$base ($n).$ext" in taken) n++
        return "$base ($n).$ext"
    }

    private fun dir(context: Context): File = File(context.filesDir, DIR)

    /** The file names of every font the user has added, sorted by name. */
    fun list(context: Context): List<String> =
        dir(context).listFiles()
            ?.filter { it.isFile && SAFE_FILE_NAME.matches(it.name) }
            ?.map { it.name }
            ?.sortedBy { it.lowercase() }
            ?: emptyList()

    /** The file behind a font id when it exists, otherwise null. */
    fun fileFor(context: Context, id: String): File? {
        val name = fileNameOf(id) ?: return null
        return File(dir(context), name).takeIf { it.isFile }
    }

    /** Loads the typeface of a stored font; null when the file is missing or cannot be read as a font. */
    fun typeface(context: Context, id: String): Typeface? {
        val file = fileFor(context, id) ?: return null
        return runCatching { Typeface.createFromFile(file) }.getOrNull()?.takeIf { it !== Typeface.DEFAULT }
    }

    /** Copies the picked font into private storage after checking its size and header. Run off the main thread. */
    fun import(context: Context, uri: Uri): ImportResult {
        return try {
            val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) return ImportResult.TooLarge
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } ?: return ImportResult.Failed
            val extension = sniffExtension(bytes) ?: return ImportResult.NotAFont
            val folder = dir(context).also { it.mkdirs() }
            val name = uniqueFileName(safeFileName(displayName, extension), list(context).toSet())
            val target = File(folder, name)
            target.writeBytes(bytes)
            val id = idFor(name)
            if (typeface(context, id) == null) {
                target.delete()
                return ImportResult.NotAFont
            }
            ImportResult.Added(id)
        } catch (e: Exception) {
            runCatching { AppLogRecorder.w("CustomFonts", "Font import failed: ${e.javaClass.simpleName}") }
            ImportResult.Failed
        }
    }

    /** Deletes a stored font. Returns true when a file was removed. */
    fun delete(context: Context, id: String): Boolean = fileFor(context, id)?.delete() == true
}
