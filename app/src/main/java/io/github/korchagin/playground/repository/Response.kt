package io.github.korchagin.playground.repository

/**
 * @author by Arthur Korchagin on 18.05.18.
 */
data class Response(val type: ResponseType, val news: News)

enum class ResponseType {
    Success,
    Pending
}