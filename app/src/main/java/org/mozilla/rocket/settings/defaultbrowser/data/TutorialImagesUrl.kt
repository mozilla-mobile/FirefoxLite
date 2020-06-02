package org.mozilla.rocket.settings.defaultbrowser.data

import org.json.JSONObject

data class TutorialImagesUrl(
    val flow1TutorialStep1ImageUrl: String,
    val flow1TutorialStep2ImageUrl: String,
    val flow2TutorialStep2ImageUrl: String
) {
    constructor(obj: JSONObject) : this(
        obj.optString("flow_1_step_1_image_url"),
        obj.optString("flow_1_step_2_image_url"),
        obj.optString("flow_2_step_2_image_url")
    )
}