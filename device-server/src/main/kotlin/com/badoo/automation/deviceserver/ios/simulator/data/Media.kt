package com.badoo.automation.deviceserver.ios.simulator.data

import com.badoo.automation.deviceserver.data.FilesDto
import com.badoo.automation.deviceserver.data.UDID
import com.badoo.automation.deviceserver.host.IRemote
import com.badoo.automation.deviceserver.util.withDefers
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

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

    fun addMedia(data: FilesDto) {
        var mediaPaths: List<String> = ArrayList()

        withDefers(logger) {
            data.files.forEach {
                val file = File(it.file_name)
                val tmpFile = File.createTempFile(file.nameWithoutExtension, ".${file.extension}")
                defer { tmpFile.delete() }
                tmpFile.writeBytes(it.data)
                mediaPaths += tmpFile.absolutePath
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
