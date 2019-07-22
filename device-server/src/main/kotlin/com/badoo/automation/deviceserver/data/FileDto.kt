package com.badoo.automation.deviceserver.data

import com.fasterxml.jackson.annotation.JsonProperty

data class FileDto(
    @JsonProperty("file_name")
    val file_name: String,

    @JsonProperty("data")
    val data: ByteArray
)

data class FilesDto(
    @JsonProperty("files")
    val files: List<FileDto>
)