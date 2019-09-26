package org.mozilla.rocket.home.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.R
import org.mozilla.rocket.nightmode.themed.ThemedImageView

class MenuButton : ConstraintLayout {
    private val menu: ThemedImageView
    private val downloadingIndicator: LottieAnimationView
    private val unreadIndicator: ImageView

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.button_menu, this)

        menu = findViewById(R.id.menu_icon)
        downloadingIndicator = findViewById(R.id.menu_downloading_indicator)
        unreadIndicator = findViewById(R.id.menu_download_unread_indicator)
    }

    fun setDownloadState(state: Int) {
        when (state) {
            DOWNLOAD_STATE_DEFAULT -> {
                unreadIndicator.visibility = View.GONE
                downloadingIndicator.visibility = View.GONE
            }
            DOWNLOAD_STATE_DOWNLOADING -> {
                unreadIndicator.visibility = View.GONE
                downloadingIndicator.apply {
                    visibility = View.VISIBLE
                    if (!downloadingIndicator.isAnimating) {
                        playAnimation()
                    }
                }
            }
            DOWNLOAD_STATE_UNREAD -> {
                unreadIndicator.apply {
                    visibility = View.VISIBLE
                    setImageResource(R.drawable.notify_download)
                }
                downloadingIndicator.visibility = View.GONE
            }
            DOWNLOAD_STATE_WARNING -> {
                unreadIndicator.apply {
                    visibility = View.VISIBLE
                    setImageResource(R.drawable.notify_notice)
                }
                downloadingIndicator.visibility = View.GONE
            }
            else -> error("Unexpected download state")
        }
    }

    fun setNightMode(isNight: Boolean) {
        menu.setNightMode(isNight)
    }

    companion object {
        const val DOWNLOAD_STATE_DEFAULT = 0
        const val DOWNLOAD_STATE_DOWNLOADING = 1
        const val DOWNLOAD_STATE_UNREAD = 2
        const val DOWNLOAD_STATE_WARNING = 3
    }
}