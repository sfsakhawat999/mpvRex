package app.marlboroadvance.mpvex.ui.browser.networkstreaming.clients

import android.net.Uri
import android.util.Log
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/**
 * SMB client implementation (SMB 2/3).
 */
class SmbClient(connection: NetworkConnection) : BaseNetworkClient(connection) {
    private var smbClient: SMBClient? = null
    private var smbConnection: Connection? = null
    private var session: Session? = null
    private var shareName: String = ""
    private var resolvedHostIp: String = ""

    companion object {
        private const val TAG = "SmbClient"
    }

    override suspend fun performConnect() {
        val config = SmbConfig.builder()
            .withTimeout(30000, TimeUnit.MILLISECONDS)
            .withSoTimeout(35000, TimeUnit.MILLISECONDS)
            .withDialects(
                com.hierynomus.mssmb2.SMB2Dialect.SMB_3_1_1,
                com.hierynomus.mssmb2.SMB2Dialect.SMB_3_0_2,
                com.hierynomus.mssmb2.SMB2Dialect.SMB_3_0,
                com.hierynomus.mssmb2.SMB2Dialect.SMB_2_1,
                com.hierynomus.mssmb2.SMB2Dialect.SMB_2_0_2,
            )
            .withDfsEnabled(false)
            .build()

        smbClient = SMBClient(config)

        val resolvedAddress = withTimeout(5000) {
            java.net.InetAddress.getByName(connection.host)
        }
        resolvedHostIp = resolvedAddress.hostAddress ?: connection.host

        shareName = connection.path.trim('/')
        if (shareName.isEmpty() || shareName.contains('/')) {
            throw Exception("Share name required. Path should be just the share name (e.g., /Media).")
        }

        smbConnection = smbClient?.connect(resolvedHostIp, connection.port)
        
        val authContext = if (connection.isAnonymous) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(connection.username, connection.password.toCharArray(), "")
        }

        session = smbConnection?.authenticate(authContext)
        
        // Test share access
        (session?.connectShare(shareName) as? DiskShare)?.close()
    }

    override suspend fun performDisconnect() {
        runCatching { session?.close() }
        runCatching { smbConnection?.close() }
        smbClient = null
        session = null
        smbConnection = null
    }

    override fun checkIsConnected(): Boolean = session != null

    override suspend fun performListFiles(path: String): List<NetworkFile> {
        val diskShare = session?.connectShare(shareName) as? DiskShare ?: throw Exception("Failed to connect to share")
        
        return try {
            val smbPath = path.trim('/').replace('/', '\\')
            val fileList = diskShare.list(smbPath)
            
            fileList.filter { it.fileName != "." && it.fileName != ".." }
                .map { info ->
                    val fileName = info.fileName
                    val filePath = if (path.isEmpty() || path == "/") fileName 
                    else "${path.trimEnd('/')}/$fileName"

                    NetworkFile(
                        name = fileName,
                        path = filePath,
                        isDirectory = info.fileAttributes and 0x10L != 0L,
                        size = info.endOfFile,
                        lastModified = info.changeTime.toEpoch(TimeUnit.MILLISECONDS),
                        mimeType = if (info.fileAttributes and 0x10L != 0L) null else getMimeType(fileName)
                    )
                }
        } finally {
            diskShare.close()
        }
    }

    override suspend fun performGetFileStream(path: String): InputStream {
        val diskShare = session?.connectShare(shareName) as? DiskShare ?: throw Exception("Failed to connect to share")
        val smbPath = path.trim('/').replace('/', '\\')
        
        val file = diskShare.openFile(
            smbPath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
        
        return file.inputStream
    }

    override suspend fun performGetFileUri(path: String): Uri {
        val cleanPath = path.trim('/')
        val uriString = if (connection.isAnonymous) {
            "smb://${connection.host}:${connection.port}/$shareName/$cleanPath"
        } else {
            "smb://${connection.username}:${connection.password}@${connection.host}:${connection.port}/$shareName/$cleanPath"
        }
        return Uri.parse(uriString)
    }

    override suspend fun performGetFileSize(path: String): Long {
        val diskShare = session?.connectShare(shareName) as? DiskShare ?: throw Exception("Failed to connect to share")
        return try {
            val smbPath = path.trim('/').replace('/', '\\')
            diskShare.getFileInformation(smbPath).standardInformation.endOfFile
        } finally {
            diskShare.close()
        }
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
