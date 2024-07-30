package wtf.zani.spice

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.glfwGetVersion
import org.lwjgl.glfw.GLFW.glfwRawMouseMotionSupported
import org.lwjgl.openal.AL10.AL_VERSION
import org.lwjgl.openal.AL10.alGetString
import org.lwjgl.system.Configuration.GLFW_CHECK_THREAD0
import org.lwjgl.system.MemoryStack
import wtf.zani.spice.debug.DebugHelper
import wtf.zani.spice.debug.DebugSection
import wtf.zani.spice.platform.api.Platform
import wtf.zani.spice.util.isMac
import wtf.zani.spice.util.isOptifineLoaded
import kotlin.io.path.*

object Spice {
    @JvmStatic
    val options: Options by lazy {
        val defaultOptions = Options(
            rawInput = glfwRawMouseMotionSupported(),
            lwjgl = LwjglOptions(
                beta = false,
                version = ""
            )
        )

        defaultOptions.needsSave = true

        if (configFile.exists()) {
            try {
                json.decodeFromString<Options>(configFile.readText())
            } catch (_: Exception) {
                defaultOptions
            }
        } else {
            defaultOptions
        }
    }

    @JvmStatic
    lateinit var version: String

    @JvmStatic
    internal val logger = LogManager.getLogger("Spice")

    @JvmStatic
    lateinit var platform: Platform
        private set

    @JvmStatic
    lateinit var glfwVersion: String
        private set

    @JvmStatic
    lateinit var openalVersion: String
        private set

    private val configDirectory = Path("spice").toAbsolutePath()
    private val configFile = configDirectory.resolve("config.json")
    private val json = Json { ignoreUnknownKeys = true }

    @JvmStatic
    fun initialize(platform: Platform) {
        this.platform = platform

        saveOptions()

        Runtime.getRuntime().addShutdownHook(Thread {
            if (options.needsSave) saveOptions()
        })

        if (isMac()) GLFW_CHECK_THREAD0.set(false)

        if (isOptifineLoaded()) logger.warn("OptiFine is enabled! No performance patches will be applied.")

        // todo: store in jar and load
        version = "1.0.0"
        glfwVersion = MemoryStack
            .stackPush()
            .use { stack ->
                val major = stack.ints(0)
                val minor = stack.ints(0)
                val patch = stack.ints(0)

                glfwGetVersion(major, minor, patch)

                "${major.get()}.${minor.get()}.${patch.get()}"
            }

        logger.info("Spice Version: $version")
        logger.info("Platform: $platform")

        initializeDebugSections()
    }

    @JvmStatic
    fun saveOptions() {
        if (!configDirectory.exists()) {
            configDirectory.createDirectories()
        }

        if (!options.needsSave) return

        configFile.writeText(json.encodeToString(options))
        options.needsSave = false
    }

    private fun initializeDebugSections() {
        val versionDebugSection = DebugSection()

        versionDebugSection.lines += { "Version: $version" }
        versionDebugSection.lines += { "GLFW: $glfwVersion" }
        versionDebugSection.lines += {
            if (!this::openalVersion.isInitialized) openalVersion = alGetString(AL_VERSION)!!

            "OpenAL: $openalVersion"
        }
        versionDebugSection.lines += { "LWJGL: ${Version.getVersion()}" }

        DebugHelper.sections += versionDebugSection
    }
}