package qdvc.cat.android.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads a text file from a content:// or file:// [Uri]. Everything here is
 * defensive: files can be huge, binary, or unreadable, and the app must never
 * crash or hang on "Open with".
 */
object FileLoader {

    /** Above this we stop reading and mark the result truncated. */
    private const val MAX_BYTES = 8 * 1024 * 1024 // 8 MB

    sealed interface Result {
        data class Text(
            val displayName: String?,
            val content: String,
            val truncated: Boolean,
        ) : Result

        data class Error(val message: String) : Result
    }

    fun load(context: Context, uri: Uri): Result {
        val name = queryDisplayName(context, uri)
        return try {
            val resolver = context.contentResolver
            resolver.openInputStream(uri).use { stream ->
                if (stream == null) return Result.Error("Could not open the file.")
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val sb = StringBuilder()
                val buf = CharArray(8192)
                var total = 0
                var truncated = false
                var nulls = 0
                var read = reader.read(buf)
                while (read != -1) {
                    // Cheap binary sniff on the first chunk: many NUL bytes.
                    if (total == 0) {
                        for (i in 0 until read) if (buf[i] == '\u0000') nulls++
                        if (nulls > 4) return Result.Error("This doesn't look like a text file.")
                    }
                    sb.append(buf, 0, read)
                    total += read
                    if (total >= MAX_BYTES) {
                        truncated = true
                        break
                    }
                    read = reader.read(buf)
                }
                Result.Text(displayName = name, content = sb.toString(), truncated = truncated)
            }
        } catch (e: SecurityException) {
            Result.Error("Permission denied reading this file.")
        } catch (e: Exception) {
            Result.Error("Could not read the file: ${e.message ?: "unknown error"}")
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) c.getString(idx) else null
                    } else null
                }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
