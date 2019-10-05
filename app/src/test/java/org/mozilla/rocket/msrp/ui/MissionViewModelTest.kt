package org.mozilla.rocket.msrp.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.TestData
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.domain.GetRedeemMissionsUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import java.util.concurrent.Executors

class MissionViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var missionViewModel: MissionViewModel
    private lateinit var missionRepository: MissionRepository
    private lateinit var userRepository: UserRepository

    private val testMissions = TestData.missions

    private val mainThreadSurrogate = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)

        missionRepository = mock(MissionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
    }

    @After
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun createLoadMissionsUseCase(): GetRedeemMissionsUseCase {

        return GetRedeemMissionsUseCase(missionRepository)
    }

    @Ignore
    @Test
    fun `When there's data, ViewModel will show data page`() {

//        Mockito.`when`(missionRepository.getMissions(Mockito.anyString()))
//                .thenReturn(Result.success(testMissions))
//
//        missionViewModel = MissionViewModel(createLoadMissionsUseCase(), createRedeemUseCase())
//
//        missionViewModel.loadMissions()
//
//        assertEquals(testMissions, LiveDataTestUtil.getValue(missionViewModel.missions))
//
//        LiveDataTestUtil.getValue(missionViewModel.challengeListViewState) is MissionViewModel.State.Idle
    }

    private fun createRedeemUseCase(): RedeemUseCase {
        return RedeemUseCase(missionRepository, userRepository)
    }

    @Ignore
    @Test
    fun `When there's no data, ViewModel will show empty page`() {

//        val emptyList = listOf<Mission>()
//
//        Mockito.`when`(missionRepository.getMissions(Mockito.anyString()))
//                .thenReturn(Result.success(emptyList()))
//
//        missionViewModel = MissionViewModel(createLoadMissionsUseCase(), createRedeemUseCase())
//
//        missionViewModel.loadMissions()
//
//        assertEquals(emptyList, LiveDataTestUtil.getValue(missionViewModel.missions))
//
//        LiveDataTestUtil.getValue(missionViewModel.challengeListViewState) is MissionViewModel.State.Empty
    }

    @Ignore
    @Test
    fun `When there's a ServerErrorException, ViewModel will show ServerError page`() {

//        Mockito.`when`(missionRepository.getMissions(MISSION_GROUP_URI))
//            .thenThrow(RewardServiceException.ServerErrorException())
//
//        missionViewModel = MissionViewModel(createLoadMissionsUseCase(), createRedeemUseCase())
//
//        missionViewModel.loadMissions()
//
//        LiveDataTestUtil.getValue(missionViewModel.challengeListViewState) is MissionViewModel.State.ServerError
    }

    @Ignore
    @Test
    fun `When there's a AuthorizationException, ViewModel will show AuthError page`() {

//        Mockito.`when`(missionRepository.getMissions(MISSION_GROUP_URI))
//            .thenThrow(RewardServiceException.AuthorizationException())
//
//        missionViewModel = MissionViewModel(createLoadMissionsUseCase(), createRedeemUseCase())
//
//        missionViewModel.loadMissions()
//
//        LiveDataTestUtil.getValue(missionViewModel.challengeListViewState) is MissionViewModel.State.AuthError
    }

    companion object {
        private const val MISSION_GROUP_URI = "http://dev-server-url-from-remote-config/v1/mission/group/xxxx"
    }
}