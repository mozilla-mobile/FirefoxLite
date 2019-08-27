package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceException

class LoadMissionsUseCase(private val missionRepository: MissionRepository) :
    UseCase<LoadMissionsUseCaseParameter, LoadMissionsUseCaseResult>() {

    /**
     * Fetch a list of [Mission]s from the server
     *
     * @param [LoadMissionsUseCaseParameter] the mission group endpoint from Firebase Remote Config
     * @return [LoadMissionsUseCaseResult] a list of [Mission]s the user can join or has joined
     * @throws RewardServiceException.ServerErrorException when there's a server error
     * @throws RewardServiceException.AuthorizationException when there's a something wrong with authorization
     */
    override suspend fun execute(parameters: LoadMissionsUseCaseParameter): LoadMissionsUseCaseResult {
        val missions = missionRepository.fetchMission(parameters.missionGroupURI)
        return LoadMissionsUseCaseResult(missions)
    }
}

data class LoadMissionsUseCaseResult(
    val items: List<Mission>
)

data class LoadMissionsUseCaseParameter(
    val missionGroupURI: String
)