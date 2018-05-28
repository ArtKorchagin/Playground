package io.github.korchagin.playground.repository

import io.reactivex.Single

/**
 * @author by Arthur Korchagin on 18.05.18.
 */

interface NetworkService {

    fun loadNews(): Single<Response>

}