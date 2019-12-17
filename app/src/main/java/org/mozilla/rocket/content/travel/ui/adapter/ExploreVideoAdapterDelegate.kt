package org.mozilla.rocket.content.travel.ui.adapter

import android.graphics.Outline
import android.graphics.Typeface
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import android.view.ViewOutlineProvider
import kotlinx.android.synthetic.main.item_travel_detail_video.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ExploreVideoAdapterDelegate(private val travelCityViewModel: TravelCityViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            ExploreVideoViewHolder(view, travelCityViewModel)
}

class ExploreVideoViewHolder(
    override val containerView: View,
    private val travelCityViewModel: TravelCityViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val exploreVideo = uiModel as VideoUiModel

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.travel_explore_item_radius)

        explore_video_image.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.setRoundRect(0, 0, view!!.width + radius, view.height, radius.toFloat())
            }
        }
        explore_video_image.clipToOutline = true

        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(exploreVideo.imageUrl)
                .into(explore_video_image)

        explore_video_length.text = exploreVideo.duration
        explore_video_title.text = exploreVideo.title
        explore_video_title.typeface = if (exploreVideo.read) Typeface.DEFAULT else Typeface.DEFAULT_BOLD

        explore_video_author.text = exploreVideo.author

        when (exploreVideo.viewCount) {
            1 -> explore_video_views.text = itemView.resources.getString(R.string.travel_content_yt_view_count_single_view)
            else -> {
                val numberFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
                } else {
                    NumberFormat.getInstance().also { it.isGroupingUsed = true }
                }

                explore_video_views.text = itemView.resources.getString(R.string.travel_content_yt_view_count, numberFormat.format(exploreVideo.viewCount))
            }
        }

        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).let {
                it.timeZone = TimeZone.getTimeZone("GMT")
                it.parse(exploreVideo.date).time
            }

            val now = System.currentTimeMillis()
            val time_past = DateUtils.getRelativeTimeSpanString(date, now, DateUtils.MINUTE_IN_MILLIS)

            explore_video_date.text = time_past
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        itemView.setOnClickListener { travelCityViewModel.onVideoClicked(exploreVideo) }
    }
}

data class VideoUiModel(
    val id: String,
    val imageUrl: String,
    val duration: String,
    val title: String,
    val author: String,
    val viewCount: Int,
    val date: String,
    val read: Boolean,
    val linkUrl: String,
    val source: String
) : DelegateAdapter.UiModel()
