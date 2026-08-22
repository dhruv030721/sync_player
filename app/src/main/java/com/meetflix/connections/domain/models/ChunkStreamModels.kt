package com.meetflix.connections.domain.models

import kotlinx.serialization.Serializable

/**
 * Design Pattern: Domain Model / Value Object
 * Immutable representation of a video chunk metadata packet transmitted over P2P Nearby Connections.
 */
@Serializable
data class VideoChunkMeta(
    val chunkIndex: Int,
    val totalChunks: Int,
    val startByte: Long,
    val chunkSize: Int,
    val totalFileSize: Long,
    val streamId: String = "default_stream"
)
