package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.util.Result

class LoadMissionsUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) : UseCase<LoadMissionsUseCaseParameter, Result<List<Mission>, RewardServiceError>>() {

    /**
     * Fetch a list of [Mission]s from the server
     *
     * @param [LoadMissionsUseCaseParameter] the mission group endpoint from Firebase Remote Config
     * @return [LoadMissionsUseCaseResult] a list of [Mission]s the user can join or has joined
     */
    override suspend fun execute(parameters: LoadMissionsUseCaseParameter): Result<List<Mission>, RewardServiceError> {
        return missionRepository.fetchMission(userRepository.getUserToken())
    }
}

data class LoadMissionsUseCaseResult(
    val items: List<Mission>
)

class LoadMissionsUseCaseParameter