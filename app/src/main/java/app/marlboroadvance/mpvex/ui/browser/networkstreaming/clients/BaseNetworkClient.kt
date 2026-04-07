package app.marlboroadvance.mpvex.ui.browser.networkstreaming.clients

import android.net.Uri
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Base class for all network protocol clients to reduce duplication.
 */
abstract class BaseNetworkClient(protected val connection: NetworkConnection) : NetworkClient {

    protected abstract suspend fun performConnect(): Unit
    protected abstract suspend fun performDisconnect(): Unit
    protected abstract fun checkIsConnected(): Boolean
    
    protected abstract suspend fun performListFiles(path: String): List<NetworkFile>
    protected abstract suspend fun performGetFileStream(path: String): InputStream
    protected abstract suspend fun performGetFileUri(path: String): Uri
    protected abstract suspend fun performGetFileSize(path: String): Long

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { performConnect() }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { performDisconnect() }
        Unit
    }

    override fun isConnected(): Boolean = checkIsConnected()

    override suspend fun listFiles(path: String): Result<List<NetworkFile>> = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext Result.failure(Exception("Not connected"))
        runCatching { performListFiles(path) }
    }

    override suspend fun getFileStream(path: String): Result<InputStream> = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext Result.failure(Exception("Not connected"))
        runCatching { performGetFileStream(path) }
    }

    override suspend fun getFileUri(path: String): Result<Uri> = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext Result.failure(Exception("Not connected"))
        runCatching { performGetFileUri(path) }
    }

    override suspend fun getFileSize(path: String): Result<Long> = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext Result.failure(Exception("Not connected"))
        runCatching { performGetFileSize(path) }
    }

    /**
     * Standard path normalization for network clients.
     */
    protected fun normalizePath(path: String): String {
        return path.trim().let { if (it.isEmpty()) "/" else it }
    }
}
