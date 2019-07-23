package com.badoo.automation.deviceserver.data

import com.fasterxml.jackson.annotation.JsonProperty

data class MediaItem(@JsonProperty("data")
                     val data: ByteArray,
                     @JsonProperty("file_name")
                     val fileName: String = "")

data class MediaDTO(@JsonProperty("media")
                    val media: List<MediaItem>?)