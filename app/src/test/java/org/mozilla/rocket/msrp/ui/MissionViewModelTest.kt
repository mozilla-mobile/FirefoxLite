package org.mozilla.rocket.msrp.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mozilla.rocket.util.LiveDataTestUtil
import org.mozilla.rocket.msrp.data.TestData
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceException
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import java.util.concurrent.Executors

class MissionViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var missionViewModel: MissionViewModel

    lateinit var missionRepository: MissionRepository

    private val testMissions = TestData.missions

    private val mainThreadSurrogate = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun createLoadMissionsUseCase(): LoadMissionsUseCase {

        return LoadMissionsUseCase(missionRepository)
    }

    @Test
    fun `When there's data, ViewModel will show data page`() {

        missionRepository = mock(MissionRepository::class.java)

        Mockito.`when`(missionRepository.fetchMission(Mockito.anyString())).thenReturn(testMissions)

        missionViewModel = MissionViewModel(createLoadMissionsUseCase())

        missionViewModel.loadMissions(MISSION_GROUP_URI)

        assertEquals(testMissions, LiveDataTestUtil.getValue(missionViewModel.missions))

        LiveDataTestUtil.getValue(missionViewModel.missionViewState) is MissionViewModel.State.Idle
    }

    @Test
    fun `When there's no data, ViewModel will show empty page`() {
        missionRepository = mock(MissionRepository::class.java)

        val emptyList = listOf<Mission>()

        Mockito.`when`(missionRepository.fetchMission(Mockito.anyString())).thenReturn(emptyList)

        missionViewModel = MissionViewModel(createLoadMissionsUseCase())

        missionViewModel.loadMissions(MISSION_GROUP_URI)

        assertEquals(emptyList, LiveDataTestUtil.getValue(missionViewModel.missions))

        LiveDataTestUtil.getValue(missionViewModel.missionViewState) is MissionViewModel.State.Empty
    }

    @Test
    fun `When there's a ServerErrorException, ViewModel will show ServerError page`() {

        missionRepository = mock(MissionRepository::class.java)

        Mockito.`when`(missionRepository.fetchMission(MISSION_GROUP_URI))
            .thenThrow(RewardServiceException.ServerErrorException())

        missionViewModel = MissionViewModel(createLoadMissionsUseCase())

        missionViewModel.loadMissions(MISSION_GROUP_URI)

        LiveDataTestUtil.getValue(missionViewModel.missionViewState) is MissionViewModel.State.ServerError
    }

    @Test
    fun `When there's a AuthorizationException, ViewModel will show AuthError page`() {

        missionRepository = mock(MissionRepository::class.java)

        Mockito.`when`(missionRepository.fetchMission(MISSION_GROUP_URI))
            .thenThrow(RewardServiceException.AuthorizationException())

        missionViewModel = MissionViewModel(createLoadMissionsUseCase())

        missionViewModel.loadMissions(MISSION_GROUP_URI)

        LiveDataTestUtil.getValue(missionViewModel.missionViewState) is MissionViewModel.State.AuthError
    }

    companion object {
        private const val MISSION_GROUP_URI = "http://dev-server-url-from-remote-config/v1/mission/group/xxxx"
    }
}