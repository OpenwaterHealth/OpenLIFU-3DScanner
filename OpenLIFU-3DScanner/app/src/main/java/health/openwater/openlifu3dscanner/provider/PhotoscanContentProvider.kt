package health.openwater.openlifu3dscanner.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import health.openwater.openlifu3dscanner.extensions.MODEL_FILENAME
import health.openwater.openlifu3dscanner.extensions.SCAN_SUBDIR
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import java.io.File
import java.io.FileNotFoundException

/**
 * ContentProvider exposing photoscan collection data for ADB access.
 *
 * Usage:
 *   # List all collections
 *   adb shell content query --uri content://health.openwater.openlifu3dscanner.photoscans/
 *
 *   # List files inside a collection
 *   adb shell content query --uri content://health.openwater.openlifu3dscanner.photoscans/collections/my_scan
 *
 *   # Read a file (pipe via adb)
 *   adb shell content read --uri "content://health.openwater.openlifu3dscanner.photoscans/collections/my_scan/file/001.jpg" > 001.jpg
 *
 * Storage root: <external-files-dir>/OpenLIFU-3DScanner/
 * Note: direct adb pull from Android/data/<package>/ is unreliable on Android 11+ due to scoped
 * storage restrictions — use the ContentProvider URIs above for guaranteed cross-device access.
 */
class PhotoscanContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "health.openwater.openlifu3dscanner.photoscans"

        private const val CODE_COLLECTIONS = 1
        private const val CODE_COLLECTION_FILES = 2
        private const val CODE_FILE = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, null, CODE_COLLECTIONS)
            addURI(AUTHORITY, "collections", CODE_COLLECTIONS)
            addURI(AUTHORITY, "collections/*", CODE_COLLECTION_FILES)
            addURI(AUTHORITY, "collections/*/file/*", CODE_FILE)
        }

        val COLLECTION_COLUMNS = arrayOf("_id", "name", "path", "photo_count", "has_model", "last_modified")
        val FILE_COLUMNS = arrayOf("_id", "name", "path", "size", "type")
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val ctx = context ?: return MatrixCursor(COLLECTION_COLUMNS)
        val modelsDir = getModelsDir(ctx)

        return when (uriMatcher.match(uri)) {
            CODE_COLLECTIONS -> queryCollections(modelsDir, projection)
            CODE_COLLECTION_FILES, CODE_FILE -> {
                val collectionName = uri.pathSegments.getOrNull(1)
                    ?: return MatrixCursor(FILE_COLUMNS)
                queryCollectionFiles(modelsDir, collectionName, projection)
            }
            else -> MatrixCursor(COLLECTION_COLUMNS)
        }
    }

    private fun queryCollections(modelsDir: File, projection: Array<out String>?): MatrixCursor {
        val columns = projection ?: COLLECTION_COLUMNS
        val cursor = MatrixCursor(columns)

        modelsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.forEachIndexed { index, dir ->
                val photoCount = dir.listFiles { f ->
                    f.extension.equals("jpg", ignoreCase = true)
                }?.size ?: 0
                val scanDir = File(dir, SCAN_SUBDIR)
                val hasModel = scanDir.exists() &&
                        scanDir.listFiles()?.any { it.name == MODEL_FILENAME } == true

                cursor.addRow(columns.map { col ->
                    when (col) {
                        "_id" -> index.toLong()
                        "name" -> dir.name
                        "path" -> dir.absolutePath
                        "photo_count" -> photoCount
                        "has_model" -> if (hasModel) 1 else 0
                        "last_modified" -> dir.lastModified()
                        else -> null
                    }
                })
            }

        return cursor
    }

    private fun queryCollectionFiles(
        modelsDir: File,
        collectionName: String,
        projection: Array<out String>?
    ): MatrixCursor {
        val columns = projection ?: FILE_COLUMNS
        val cursor = MatrixCursor(columns)
        val dir = File(modelsDir, collectionName)
        if (!dir.exists()) return cursor

        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.forEachIndexed { index, file ->
                cursor.addRow(columns.map { col ->
                    when (col) {
                        "_id" -> index.toLong()
                        "name" -> file.name
                        "path" -> file.absolutePath
                        "size" -> file.length()
                        "type" -> if (file.isDirectory) "directory" else "file"
                        else -> null
                    }
                })
            }

        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        // URI: content://authority/collections/<collection>/file/<filename>
        val segments = uri.pathSegments
        if (segments.size < 4 || segments[0] != "collections" || segments[2] != "file") {
            throw FileNotFoundException("Invalid URI: $uri")
        }
        val ctx = context ?: throw FileNotFoundException("No context")
        val collectionName = segments[1]
        val filename = segments[3]
        val file = File(File(getModelsDir(ctx), collectionName), filename)
        if (!file.exists()) throw FileNotFoundException("File not found: ${file.absolutePath}")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        CODE_COLLECTIONS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.collection"
        CODE_COLLECTION_FILES -> "vnd.android.cursor.dir/vnd.$AUTHORITY.file"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
