package io.github.korchagin.playground.repository

import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

/**
 * @author by Arthur Korchagin on 18.05.18.
 */
class NewsRepository(private val networkService: NetworkService) {

    fun loadNews(requestsCount: Long, timeout: Long): Single<Response> =
            { networkService.loadNews() }.poolingRequest(requestsCount, timeout)

    private fun (() -> Single<Response>).poolingRequest(requestsCount: Long, timeout: Long) =

            Observable.intervalRange(0, requestsCount, 0, timeout, TimeUnit.SECONDS)

                    .flatMap { requestIndex ->

                        this()
                                .toObservable()

                                .onErrorResumeNext { throwable: Throwable ->

                                    if (requestIndex == requestsCount - 1) {
                                        Observable.error(throwable)
                                    } else {
                                        Observable.empty()
                                    }
                                }
                    }

                    .take(1)

                    .repeat()

                    .filter { it.type == ResponseType.Success }

                    .firstOrError()
}
