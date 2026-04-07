package app.marlboroadvance.mpvex.ui.browser.networkstreaming.clients

import android.net.Uri
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.DavResource
import java.io.InputStream

/**
 * WebDAV client implementation.
 */
class WebDavClient(connection: NetworkConnection) : BaseNetworkClient(connection) {
    private var sardine: Sardine? = null

    private fun buildUrl(relativePath: String): String {
        val protocol = if (connection.useHttps) "https" else "http"
        val basePath = connection.path.trim('/')
        val cleanPath = relativePath.trim('/')
        
        return when {
            cleanPath.isEmpty() || cleanPath == "/" -> {
                if (basePath.isEmpty()) "$protocol://${connection.host}:${connection.port}/"
                else "$protocol://${connection.host}:${connection.port}/$basePath"
            }
            basePath.isEmpty() -> "$protocol://${connection.host}:${connection.port}/$cleanPath"
            else -> "$protocol://${connection.host}:${connection.port}/$basePath/$cleanPath"
        }
    }

    override suspend fun performConnect() {
        val client = OkHttpSardine()
        if (!connection.isAnonymous) {
            client.setCredentials(connection.username, connection.password)
        }
        client.exists(buildUrl(""))
        sardine = client
    }

    override suspend fun performDisconnect() {
        sardine = null
    }

    override fun checkIsConnected(): Boolean = sardine != null

    override suspend fun performListFiles(path: String): List<NetworkFile> {
        val client = sardine ?: throw Exception("Not connected")
        val url = buildUrl(path)
        val resources = client.list(url)

        return resources.drop(1).map { resource: DavResource ->
            val resourceName = resource.name ?: ""
            val filePath = if (path.isEmpty() || path == "/") resourceName
            else "${path.trimEnd('/')}/$resourceName"

            NetworkFile(
                name = resourceName,
                path = filePath,
                isDirectory = resource.isDirectory,
                size = resource.contentLength ?: 0,
                lastModified = resource.modified?.time ?: 0,
                mimeType = if (resource.isDirectory) null else getMimeType(resourceName)
            )
        }
    }

    override suspend fun performGetFileStream(path: String): InputStream {
        val client = sardine ?: throw Exception("Not connected")
        return client.get(buildUrl(path))
    }

    override suspend fun performGetFileUri(path: String): Uri {
        val protocol = if (connection.useHttps) "https" else "http"
        val basePath = connection.path.trim('/')
        val cleanPath = path.trim('/')
        
        val fullPath = when {
            cleanPath.isEmpty() -> basePath
            basePath.isEmpty() -> cleanPath
            else -> "$basePath/$cleanPath"
        }

        val uriString = if (connection.isAnonymous) {
            "$protocol://${connection.host}:${connection.port}/$fullPath"
        } else {
            "$protocol://${connection.username}:${connection.password}@${connection.host}:${connection.port}/$fullPath"
        }
        return Uri.parse(uriString)
    }

    override suspend fun performGetFileSize(path: String): Long {
        val client = sardine ?: throw Exception("Not connected")
        val resources = client.list(buildUrl(path))
        return resources.firstOrNull()?.contentLength ?: 0L
    }

    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "mpeg", "mpg" -> "video/mpeg"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            else -> null
        }
    }
}
