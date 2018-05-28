package io.github.korchagin.playground.repository

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author by Arthur Korchagin on 21.05.18.
 */
class NewsRepositoryTest {

    companion object {
        private const val REQUESTS_COUNT = 3L
        private const val TIMEOUT = 3L
        private const val TEST_ERROR = "Test error"
    }

    private lateinit var mTestObserver: TestObserver<Response>
    private lateinit var mTestScheduler: TestScheduler

    private val ResponseType.response: Response
        get() = Response(this, News(""))


    @Before
    fun setUp() {
        mTestObserver = TestObserver.create<Response>()
        mTestScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { mTestScheduler }
    }

    @Test
    fun `Load news success`() {

        val repository = buildRepository {
            +ResponseType.Success.response
        }

        repository.loadNews(REQUESTS_COUNT, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        mTestObserver.assertValue { it.type == ResponseType.Success }

    }

    @Test
    fun `Load news only error`() {

        val repository = buildRepository {
            +Error(TEST_ERROR)
        }

        repository.loadNews(1, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        mTestObserver.assertError { it.message == TEST_ERROR }

    }

    @Test
    fun `Load news error - success`() {

        val repository = buildRepository {
            +Error(TEST_ERROR)
            +ResponseType.Success.response
        }

        repository.loadNews(REQUESTS_COUNT, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        mTestObserver.assertNoValues()

        mTestScheduler.advanceTimeBy(TIMEOUT, TimeUnit.SECONDS)

        mTestObserver.assertValue { it.type == ResponseType.Success }

    }

    @Test
    fun `Load news max errors and success`() {
        val repository = buildRepository {

            for (i in 1..(REQUESTS_COUNT - 1)) {
                +Error(TEST_ERROR)
            }

            +ResponseType.Success.response
        }

        repository.loadNews(REQUESTS_COUNT, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        for (i in 1..(REQUESTS_COUNT - 1)) {
            mTestObserver.assertNoValues()
            mTestScheduler.advanceTimeBy(TIMEOUT, TimeUnit.SECONDS)
        }

        mTestObserver.assertValue { it.type == ResponseType.Success }

    }

    @Test
    fun `Load news errors - no success`() {

        val repository = buildRepository {

            for (i in 1..REQUESTS_COUNT) {
                +Error(TEST_ERROR)
            }

            +ResponseType.Success.response
        }

        repository.loadNews(REQUESTS_COUNT, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        mTestObserver.assertNoValues()

        for (i in 1..REQUESTS_COUNT) {
            mTestScheduler.advanceTimeBy(TIMEOUT, TimeUnit.SECONDS)
            mTestObserver.assertNoValues()
        }

        mTestObserver.assertError { it.message == TEST_ERROR }

    }

    @Test
    fun `Load news max pendings and success`() {
        val repository = buildRepository {

            for (i in 1..(REQUESTS_COUNT + 1)) {
                println("-> Add Pending")
                +ResponseType.Pending.response
            }

            println("-> Add Success")
            +ResponseType.Success.response
        }

        println("-> Test ->startTime=${mTestScheduler.now(TimeUnit.SECONDS)} timeout=$TIMEOUT")

        repository.loadNews(REQUESTS_COUNT, TIMEOUT)
                .subscribe(mTestObserver)

        mTestScheduler.triggerActions()

        for (i in 1..(REQUESTS_COUNT + 1)) {

            println("-> Assert Pending -> ${mTestScheduler.now(TimeUnit.SECONDS)}")

            mTestObserver.assertNoValues()

            mTestScheduler.triggerActions()
//            mTestScheduler.advanceTimeBy(TIMEOUT, TimeUnit.SECONDS)
        }

        println("-> Assert Success")
        mTestObserver.assertValue { it.type == ResponseType.Success }

    }

    private fun buildRepository(block: StubNetworkService.() -> Unit) =
            NewsRepository(StubNetworkService().apply(block))

    class StubNetworkService : NetworkService {

        private val mutableList: MutableList<Single<Response>> = LinkedList()
        private var currentSingle = 0

        operator fun Response.unaryPlus() =
                mutableList.add(Single.just(this))

        operator fun Error.unaryPlus() =
                mutableList.add(Single.error(this))

        override fun loadNews(): Single<Response> = mutableList[currentSingle]
                .doOnSubscribe { currentSingle++ }

    }

}