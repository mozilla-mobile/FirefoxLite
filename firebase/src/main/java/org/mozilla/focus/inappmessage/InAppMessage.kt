package org.mozilla.focus.inappmessage

class InAppMessage(inAppMessage: com.google.firebase.inappmessaging.model.InAppMessage) {

    val campaignMetadata: CampaignMetadata? = inAppMessage.campaignMetadata?.let { CampaignMetadata(it) }

    class CampaignMetadata(private val campaignMetadata: com.google.firebase.inappmessaging.model.CampaignMetadata) {
        val campaignName: String
            get() = campaignMetadata.campaignName
    }

    data class Action(private val action: com.google.firebase.inappmessaging.model.Action) {
        val buttonText: String?
            get() = action.button?.text?.text

        val actionUrl: String?
            get() = action.actionUrl
    }
}