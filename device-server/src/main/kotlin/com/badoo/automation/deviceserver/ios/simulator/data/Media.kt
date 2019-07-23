package com.badoo.automation.deviceserver.ios.simulator.data

import com.badoo.automation.deviceserver.data.FilesDto
import com.badoo.automation.deviceserver.data.MediaDTO
import com.badoo.automation.deviceserver.data.UDID
import com.badoo.automation.deviceserver.host.IRemote
import com.badoo.automation.deviceserver.util.withDefers
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.nio.file.Files



class Media(
    private val remote: IRemote,
    private val udid: UDID,
    deviceSetPath: String
) {
    private val mediaPath = Paths.get(deviceSetPath, udid, "data", "Media")
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun reset() {
        val removeCmd = "rm -rf $mediaPath"

        val result = remote.shell(removeCmd)

        if (!result.isSuccess) {
            throw RuntimeException("Could not reset Media: $result")
        }

        // restart assetsd to prevent fbsimctl upload failing with Error Domain=NSCocoaErrorDomain Code=-1 \"(null)\"
        restartAssetsd()
    }

    fun addMedia(data: MediaDTO) {
        var mediaPath: String
        val tempDir = Files.createTempDirectory(null)

        withDefers(logger) {
            data.media?.forEach {
                val file = File("${tempDir.toAbsolutePath()}${it.fileName}")
                val tmpFile = file.createNewFile()
                defer { tmpFile.delete() }
                tmpFile.writeBytes(it.data)
                mediaPath = tempDir.listFiles()
            }
            val command: String = if (remote.isLocalhost()) {
                mediaPaths.joinToString(separator = " ")
            } else {
                val remoteMediaDir = remote.execIgnoringErrors(listOf("mktemp", "-d")).stdOut.trim()
                defer { remote.execIgnoringErrors(listOf("rm", "-rf", remoteMediaDir)) }
                mediaPaths.forEach() { remote.rsync(File(it).absolutePath, remoteMediaDir, setOf("-r", "--delete")) }
                mediaPaths.joinToString(separator = " ") { File(remoteMediaDir).absolutePath + File.separator + it }
            }

            val result = remote.execIgnoringErrors(listOf("xcrun", "simctl", "addmedia", udid, command))

            if (!result.isSuccess) {
                throw RuntimeException("Could not add Media to device: $result")
            }
        }
    }

    private fun restartAssetsd() {
        val restartCmd = listOf(
            "xcrun", "simctl", "spawn", udid, "launchctl", "kickstart", "-k", "-p", "system/com.apple.assetsd"
        )

        val result = remote.execIgnoringErrors(restartCmd)

        if (!result.isSuccess) {
            throw RuntimeException("Could not restart assetsd service: $result")
        }
    }
}
