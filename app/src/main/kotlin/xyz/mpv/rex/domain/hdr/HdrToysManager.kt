package xyz.mpv.rex.domain.hdr

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class HdrToysManager(private val context: Context) {

  companion object {
    private const val TAG = "HdrToysManager"
    private const val SHADER_DIR = "shaders"
    private const val HDR_TOYS_SUBDIR = "hdr-toys"
  }

  enum class ToneMapping(val fileName: String) {
    ASTRA("astra.glsl"),
    BT2390("bt2390.glsl"),
    REINHARD("reinhard.glsl"),
    LINEAR("linear.glsl")
  }

  enum class GamutMapping(val fileName: String) {
    BOTTOSSON("bottosson.glsl"),
    CLIP("clip.glsl"),
    JEDYPOD("jedypod.glsl")
  }

  private var shaderDir: File? = null
  private var isInitialized = false

  /**
   * Copy hdr-toys shaders from assets to internal storage.
   */
  fun initialize(): Boolean {
    if (isInitialized) {
      return true
    }

    return try {
      shaderDir = File(context.filesDir, SHADER_DIR)
      val hdrToysDir = File(shaderDir, HDR_TOYS_SUBDIR)
      if (!hdrToysDir.exists()) {
        hdrToysDir.mkdirs()
      }

      // Copy assets folder recursively
      val success = copyAssetsDirRecursive("shaders/$HDR_TOYS_SUBDIR", hdrToysDir)
      if (success) {
        isInitialized = true
        Log.d(TAG, "hdr-toys shaders successfully initialized in internal storage")
      }
      success
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize hdr-toys shaders", e)
      isInitialized = false
      false
    }
  }

  private fun copyAssetsDirRecursive(assetPath: String, localDir: File): Boolean {
    try {
      val list = context.assets.list(assetPath) ?: return false
      if (!localDir.exists()) {
        localDir.mkdirs()
      }
      for (name in list) {
        val subAssetPath = "$assetPath/$name"
        val subLocalFile = File(localDir, name)
        
        val subList = context.assets.list(subAssetPath)
        if (subList != null && subList.isNotEmpty()) {
          copyAssetsDirRecursive(subAssetPath, subLocalFile)
        } else {
          try {
            context.assets.open(subAssetPath).use { input ->
              FileOutputStream(subLocalFile).use { output ->
                input.copyTo(output)
              }
            }
          } catch (e: Exception) {
            if (subList != null) {
              subLocalFile.mkdirs()
            }
          }
        }
      }
      return true
    } catch (e: Exception) {
      return false
    }
  }

  /**
   * Configures mpv.conf to inject or strip the hdr-toys conditional profile.
   */
  fun configureMpvConf(filesDir: File, isEnabled: Boolean, tone: ToneMapping, gamut: GamutMapping) {
    val mpvConfFile = File(filesDir, "mpv.conf")
    if (!mpvConfFile.exists()) {
      try {
        mpvConfFile.createNewFile()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create mpv.conf", e)
        return
      }
    }

    try {
      var content = mpvConfFile.readText()
      
      // Strip any existing auto-generated hdr-toys block
      val startMarker = "# --- Begin hdr-toys Auto-generated Profile ---"
      val endMarker = "# --- End hdr-toys Auto-generated Profile ---"
      
      if (content.contains(startMarker) && content.contains(endMarker)) {
        val before = content.substringBefore(startMarker)
        val after = content.substringAfter(endMarker)
        content = before.trim() + "\n" + after.trim()
      }
      
      content = content.trim()

      if (isEnabled) {
        // Build the profile block
        val baseDir = File(shaderDir ?: File(context.filesDir, SHADER_DIR), HDR_TOYS_SUBDIR)
        val clipBoth = File(File(baseDir, "utils"), "clip_both.glsl").absolutePath
        val pqInv = File(File(baseDir, "transfer-function"), "pq_inv.glsl").absolutePath
        val toneGlsl = File(File(baseDir, "tone-mapping"), tone.fileName).absolutePath
        val gamutGlsl = File(File(baseDir, "gamut-mapping"), gamut.fileName).absolutePath
        val bt1886 = File(File(baseDir, "transfer-function"), "bt1886.glsl").absolutePath

        // Initialize shaders to make sure they exist in storage
        initialize()

        val block = """
          |
          |$startMarker
          |[bt.2100-pq]
          |profile-cond=get("video-params/primaries") == "bt.2020" and get("video-params/gamma") == "pq"
          |profile-restore=copy
          |target-prim=bt.2020
          |target-trc=pq
          |glsl-shader=$clipBoth
          |glsl-shader=$pqInv
          |glsl-shader=$toneGlsl
          |glsl-shader=$gamutGlsl
          |glsl-shader=$bt1886
          |$endMarker
        """.trimMargin()
        
        content = content + "\n" + block
      }

      mpvConfFile.writeText(content.trim() + "\n")
      Log.d(TAG, "Configured mpv.conf with enableHdrToys=$isEnabled")
    } catch (e: Exception) {
      Log.e(TAG, "Error configuring mpv.conf for hdr-toys", e)
    }
  }

  /**
   * Generates the shader chain for the specified ToneMapping and GamutMapping modes.
   * Returns a colon-separated string of absolute paths to the shaders.
   */
  fun getShaderChain(tone: ToneMapping, gamut: GamutMapping): String {
    if (!isInitialized && !initialize()) {
      return ""
    }

    val baseDir = File(shaderDir, HDR_TOYS_SUBDIR)
    if (!baseDir.exists()) {
      return ""
    }

    val shaders = mutableListOf<String>()

    // 1. clip_both
    shaders.add(File(File(baseDir, "utils"), "clip_both.glsl").absolutePath)
    
    // 2. pq_inv
    shaders.add(File(File(baseDir, "transfer-function"), "pq_inv.glsl").absolutePath)

    // 3. tone mapping
    shaders.add(File(File(baseDir, "tone-mapping"), tone.fileName).absolutePath)

    // 4. gamut mapping
    shaders.add(File(File(baseDir, "gamut-mapping"), gamut.fileName).absolutePath)

    // 5. bt1886
    shaders.add(File(File(baseDir, "transfer-function"), "bt1886.glsl").absolutePath)

    // Verify files exist
    val missing = shaders.filter { !File(it).exists() }
    if (missing.isNotEmpty()) {
      Log.e(TAG, "Missing hdr-toys shaders: $missing")
      return ""
    }

    return shaders.joinToString(":")
  }
}
