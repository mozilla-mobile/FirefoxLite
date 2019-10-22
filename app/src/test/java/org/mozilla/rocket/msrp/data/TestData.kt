package org.mozilla.rocket.msrp.data

object TestData {

    val mission01 = Mission(
        mid = "000001",
        title = "TypeDaily Mission 1",
        description = "Click vertical everyday",

        events = listOf("CLICK_PANEL_PIN_TOP_SITE"),

        important = true,
        status = 0, // 0: new , 1: joined 2. redeem,

        endpoint = "/v1/daily_mission/xxdase-eadsad",
        redeem = "/v1/redeem/asdsa-esadsa=das-dased-sadas",

        minVersion = 1234,
        missionType = "mission_daily",
        imageUrl = "",
        expiredDate = 12345,
        joinEndDate = 54321,
        redeemEndDate = 54321,
        rewardExpiredDate = 54321,

        missionProgress = null
    )

    val mission02 = Mission(
        mid = "000002",
        title = "TypeDaily Mission 1",
        description = "Click vertical everyday",

        events = listOf("CLICK_PANEL_PIN_TOP_SITE"),

        important = true,
        status = 2, // 0: new , 1: joined 2. redeem,
        endpoint = "/v1/daily_mission/xxdase-eadsad",
        redeem = "/v1/redeem/asdsa-esadsa=das-dased-sadas",

        minVersion = 4321,
        missionType = "mission_daily",
        imageUrl = "",
        expiredDate = 12345,
        joinEndDate = 54321,
        redeemEndDate = 54321,
        rewardExpiredDate = 54321,

        missionProgress = MissionProgress.TypeDaily(
            // progress, TODO: make it another class
            joinDate = 0,
            currentDay = 1,
            totalDays = 10,
            message = ""
        )
    )

    val missions = listOf(
        mission01,
        mission02
    )
}