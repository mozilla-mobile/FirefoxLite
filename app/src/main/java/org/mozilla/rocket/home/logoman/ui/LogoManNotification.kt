package org.mozilla.rocket.home.logoman.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.home_notification_board.notification_icon
import kotlinx.android.synthetic.main.home_notification_board.notification_subtitle
import kotlinx.android.synthetic.main.home_notification_board.notification_title
import kotlinx.android.synthetic.main.logo_man_notification.view.logo_man
import kotlinx.android.synthetic.main.logo_man_notification.view.notification_board
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.extension.dpToPx
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification.MissionNotification
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification.RemoteNotification
import kotlin.math.abs

class LogoManNotification : FrameLayout {

    private lateinit var adapter: DelegateAdapter
    private var actionListener: NotificationActionListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        initViews()
    }

    private fun initViews() {
        inflate(context, R.layout.logo_man_notification, this)
        minimumHeight = dpToPx(MIN_HEIGHT_IN_DP)
        initNotificationBoard()
    }

    private fun initNotificationBoard() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(RemoteNotification::class, R.layout.home_notification_board, RemoteNotificationAdapterDelegate { actionListener?.onNotificationClick() })
                add(MissionNotification::class, R.layout.home_notification_board_mission, MissionNotificationAdapterDelegate { actionListener?.onNotificationClick() })
            }
        )
        notification_board.apply {
            adapter = this@LogoManNotification.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }

        val swipeFlag = ItemTouchHelper.START or ItemTouchHelper.END
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, swipeFlag) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                notification_board.visibility = View.GONE
                startSwipeOut()
                actionListener?.onNotificationDismiss()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val alpha = 1f - abs(dX) / (recyclerView.width / 2f)
                    viewHolder.itemView.alpha = alpha
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(notification_board)
    }

    fun showNotification(notification: Notification, animate: Boolean) {
        adapter.setData(listOf(notification))
        if (animate) {
            startSwipeIn()
        } else {
            logo_man.translationY = dpToPx(LOGO_MAN_SWIPE_IN_3_END_Y_IN_DP).toFloat()
            notification_board.translationY = dpToPx(NOTIFICATION_BOARD_SWIPE_IN_2_END_Y_IN_DP).toFloat()
        }
    }

    private fun startSwipeIn() {
        val logoManListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            logo_man.translationY = value.toFloat()
        }
        val notificationBoardListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            notification_board.translationY = value.toFloat()
        }
        val logoManMoveInAnimatorSet = AnimatorSet().apply {
            val logoManSwipeInAnimator1 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_1_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_1_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_1_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManSwipeInAnimator2 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_2_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_2_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_2_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManSwipeInAnimator3 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_3_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_3_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_3_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            playSequentially(logoManSwipeInAnimator1, logoManSwipeInAnimator2, logoManSwipeInAnimator3)
        }
        val notificationBoardMoveInAnimatorSet = AnimatorSet().apply {
            val notificationBoardMoveInAnimator1 = ValueAnimator.ofInt(
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_1_START_Y_IN_DP),
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_1_END_Y_IN_DP)
            ).apply {
                duration = NOTIFICATION_BOARD_SWIPE_IN_1_DURATION_IN_MS
                addUpdateListener(notificationBoardListener)
            }
            val notificationBoardMoveInAnimator2 = ValueAnimator.ofInt(
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_2_START_Y_IN_DP),
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_2_END_Y_IN_DP)
            ).apply {
                duration = NOTIFICATION_BOARD_SWIPE_IN_2_DURATION_IN_MS
                addUpdateListener(notificationBoardListener)
            }
            playSequentially(notificationBoardMoveInAnimator1, notificationBoardMoveInAnimator2)
        }
        AnimatorSet().apply {
            this.play(logoManMoveInAnimatorSet)
                    .with(notificationBoardMoveInAnimatorSet)
                    .after(1500)
        }.start()
    }

    private fun startSwipeOut() {
        val logoManListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            logo_man.translationY = value.toFloat()
        }
        AnimatorSet().apply {
            val logoManMoveOutAnimator1 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_OUT_1_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_OUT_1_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_OUT_1_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManMoveOutAnimator2 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_OUT_2_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_OUT_2_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_OUT_2_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            playSequentially(logoManMoveOutAnimator1, logoManMoveOutAnimator2)
        }.start()
    }

    fun setNotificationActionListener(listener: NotificationActionListener) {
        actionListener = listener
    }

    private class RemoteNotificationAdapterDelegate(private val clickListener: () -> Unit) : AdapterDelegate {
        override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
                RemoteNotificationViewHolder(view, clickListener)
    }

    private class MissionNotificationAdapterDelegate(private val clickListener: () -> Unit) : AdapterDelegate {
        override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
                MissionNotificationViewHolder(view, clickListener)
    }

    private class RemoteNotificationViewHolder(
        override val containerView: View,
        private val clickListener: () -> Unit
    ) : DelegateAdapter.ViewHolder(containerView) {
        override fun bind(uiModel: DelegateAdapter.UiModel) {
            uiModel as RemoteNotification
            notification_title.text = uiModel.title
            notification_subtitle.text = uiModel.subtitle
            if (uiModel.subtitle.isNullOrEmpty()) {
                notification_title.maxLines = 3
            } else {
                notification_title.maxLines = 1
            }
            itemView.setOnClickListener { clickListener() }

            if (uiModel.imageUrl != null) {
                notification_icon.isVisible = true
                GlideApp.with(itemView.context)
                        .asBitmap()
                        .centerCrop()
                        .load(uiModel.imageUrl)
                        .into(notification_icon)
            } else {
                notification_icon.isVisible = false
                notification_icon.setImageDrawable(null)
            }
        }
    }

    private class MissionNotificationViewHolder(
        override val containerView: View,
        private val clickListener: () -> Unit
    ) : DelegateAdapter.ViewHolder(containerView) {

        private val imageSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, containerView.resources.displayMetrics).toInt()

        override fun bind(uiModel: DelegateAdapter.UiModel) {
            uiModel as MissionNotification
            notification_title.text = uiModel.title
            notification_subtitle.text = uiModel.subtitle
            if (uiModel.subtitle.isNullOrEmpty()) {
                notification_title.maxLines = 3
            } else {
                notification_title.maxLines = 1
            }
            itemView.setOnClickListener { clickListener() }

            val context = containerView.context
            notification_icon.setImageResource(R.drawable.ic_reward_box)
            Glide.with(context)
                    .asBitmap()
                    .load(uiModel.imageUrl)
                    .apply(RequestOptions().transform(CircleCrop()))
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                            notification_icon.setImageBitmap(getRewardImage(context, imageSize, imageSize, resource))
                        }
                    })
        }

        private fun getRewardImage(context: Context, width: Int, height: Int, imageBitmap: Bitmap): Bitmap {
            val imageSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22f, context.resources.displayMetrics).toInt()
            val shiftX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, context.resources.displayMetrics).toInt()
            val shiftY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics).toInt()

            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val couponBitmap = DrawableUtils.getBitmap(context.getDrawable(R.drawable.ic_reward_box))
            val canvas = Canvas(resultBitmap)
            val paint = Paint()
            canvas.drawBitmap(couponBitmap, 0f, 0f, paint)
            val centerX = width / 2 + shiftX
            val centerY = height / 2 + shiftY
            val src = Rect(0, 0, imageBitmap.width, imageBitmap.height)
            val target = Rect(
                centerX - imageSize / 2,
                centerY - imageSize / 2,
                centerX + imageSize / 2,
                centerY + imageSize / 2
            )
            canvas.drawBitmap(imageBitmap, src, target, paint)

            couponBitmap.recycle()

            return resultBitmap
        }
    }

    sealed class Notification(
        val id: String,
        val title: String,
        val subtitle: String?,
        val imageUrl: String?
    ) : DelegateAdapter.UiModel() {
        class RemoteNotification(id: String, title: String, subtitle: String?, imageUrl: String?) : Notification(id, title, subtitle, imageUrl)

        class MissionNotification(id: String, title: String, subtitle: String?, imageUrl: String?) : Notification(id, title, subtitle, imageUrl)
    }

    interface NotificationActionListener {
        fun onNotificationClick()
        fun onNotificationDismiss()
    }

    companion object {
        private const val MIN_HEIGHT_IN_DP = 136f

        private const val LOGO_MAN_SWIPE_IN_1_DURATION_IN_MS = 270L
        private const val LOGO_MAN_SWIPE_IN_1_START_Y_IN_DP = 144f
        private const val LOGO_MAN_SWIPE_IN_1_END_Y_IN_DP = 41f

        private const val LOGO_MAN_SWIPE_IN_2_DURATION_IN_MS = 100L
        private const val LOGO_MAN_SWIPE_IN_2_START_Y_IN_DP = 41f
        private const val LOGO_MAN_SWIPE_IN_2_END_Y_IN_DP = 31f

        private const val LOGO_MAN_SWIPE_IN_3_DURATION_IN_MS = 130L
        private const val LOGO_MAN_SWIPE_IN_3_START_Y_IN_DP = 31f
        private const val LOGO_MAN_SWIPE_IN_3_END_Y_IN_DP = 36f

        private const val LOGO_MAN_SWIPE_OUT_1_DURATION_IN_MS = 160L
        private const val LOGO_MAN_SWIPE_OUT_1_START_Y_IN_DP = 36f
        private const val LOGO_MAN_SWIPE_OUT_1_END_Y_IN_DP = 10f

        private const val LOGO_MAN_SWIPE_OUT_2_DURATION_IN_MS = 270L
        private const val LOGO_MAN_SWIPE_OUT_2_START_Y_IN_DP = 10f
        private const val LOGO_MAN_SWIPE_OUT_2_END_Y_IN_DP = 144f

        private const val NOTIFICATION_BOARD_SWIPE_IN_1_DURATION_IN_MS = 300L
        private const val NOTIFICATION_BOARD_SWIPE_IN_1_START_Y_IN_DP = 75f
        private const val NOTIFICATION_BOARD_SWIPE_IN_1_END_Y_IN_DP = -60f

        private const val NOTIFICATION_BOARD_SWIPE_IN_2_DURATION_IN_MS = 100L
        private const val NOTIFICATION_BOARD_SWIPE_IN_2_START_Y_IN_DP = -60f
        private const val NOTIFICATION_BOARD_SWIPE_IN_2_END_Y_IN_DP = -56f
    }
}