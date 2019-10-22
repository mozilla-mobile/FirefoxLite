package org.mozilla.rocket.msrp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_mission_detail.congrats_title_layout
import kotlinx.android.synthetic.main.fragment_mission_detail.date_layout_row_1
import kotlinx.android.synthetic.main.fragment_mission_detail.date_layout_row_2
import kotlinx.android.synthetic.main.fragment_mission_detail.how_to_redeem
import kotlinx.android.synthetic.main.fragment_mission_detail.image
import kotlinx.android.synthetic.main.fragment_mission_detail.join_button
import kotlinx.android.synthetic.main.fragment_mission_detail.join_layout
import kotlinx.android.synthetic.main.fragment_mission_detail.join_layout_separator
import kotlinx.android.synthetic.main.fragment_mission_detail.join_terms
import kotlinx.android.synthetic.main.fragment_mission_detail.loading_view
import kotlinx.android.synthetic.main.fragment_mission_detail.mission_step_text_1
import kotlinx.android.synthetic.main.fragment_mission_detail.quit_button
import kotlinx.android.synthetic.main.fragment_mission_detail.quit_button_separator
import kotlinx.android.synthetic.main.fragment_mission_detail.redeem_button
import kotlinx.android.synthetic.main.fragment_mission_detail.redeem_later_button
import kotlinx.android.synthetic.main.fragment_mission_detail.redeem_layout
import kotlinx.android.synthetic.main.fragment_mission_detail.title
import kotlinx.android.synthetic.main.fragment_mission_detail.view.day_text
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.appContext
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.extension.showToast
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.worker.DailyMissionReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class MissionDetailFragment : Fragment(), NavigationResult {

    private val safeArgs: MissionDetailFragmentArgs by navArgs()
    private val mission by lazy { safeArgs.mission }
    private lateinit var missionViewModel: MissionViewModel
    private lateinit var missionDetailViewModel: MissionDetailViewModel

    @Inject
    lateinit var missionViewModelCreator: Lazy<MissionViewModel>
    @Inject
    lateinit var missionDetailViewModelCreator: Lazy<MissionDetailViewModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        missionViewModel = getActivityViewModel(missionViewModelCreator)
        missionDetailViewModel = getViewModel(missionDetailViewModelCreator)
        missionDetailViewModel.init(mission)
        missionDetailViewModel.onMissionDetailViewed()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_mission_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        bindViews()
        observeActions()
    }

    private fun initViews() {
        mission_step_text_1.text = getString(R.string.msrp_challenge_details_body_1, getString(R.string.app_name))
        join_button.setOnClickListener {
            if (missionDetailViewModel.isLoading.value != true) {
                missionDetailViewModel.onJoinMissionButtonClicked()
            }
        }
        quit_button.setOnClickListener {
            if (missionDetailViewModel.isLoading.value != true) {
                missionDetailViewModel.onQuitMissionButtonClicked()
            }
        }
        redeem_button.setOnClickListener {
            if (missionDetailViewModel.isLoading.value != true) {
                missionDetailViewModel.onRedeemButtonClicked()
            }
        }
        redeem_later_button.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindViews() {
        missionDetailViewModel.missionStatus.observe(this, Observer { status ->
            when (status) {
                Mission.STATUS_NEW -> showUnjoinMission()
                Mission.STATUS_JOINED -> showJoinedMission(mission)
                Mission.STATUS_REDEEMABLE -> showRedeemableMission(mission)
            }
        })
        missionDetailViewModel.title.observe(this, Observer {
            title.text = it
        })
        missionDetailViewModel.missionImage.observe(this, Observer { url ->
            Glide.with(requireContext())
                    .load(url)
                    .apply(RequestOptions().apply { transforms(CircleCrop()) })
                    .into(image)
        })
        missionDetailViewModel.isLoading.observe(this, Observer {
            loading_view.isVisible = it
        })
    }

    private fun showUnjoinMission() {
        title.isVisible = true
        join_layout.isVisible = true
        join_button.isVisible = true
        join_terms.isVisible = true
        join_layout_separator.isVisible = false
        how_to_redeem.isVisible = false

        quit_button.isVisible = false
        quit_button_separator.isVisible = false

        redeem_layout.isVisible = false
        congrats_title_layout.isVisible = false
    }

    private fun showJoinedMission(mission: Mission) {
        title.isVisible = true
        join_layout.isVisible = true
        join_button.isVisible = false
        join_terms.isVisible = false
        join_layout_separator.isVisible = true
        how_to_redeem.isVisible = true

        quit_button.isVisible = true
        quit_button_separator.isVisible = true

        redeem_layout.isVisible = true
        redeem_later_button.isVisible = false
        redeem_button.isEnabled = false
        congrats_title_layout.isVisible = false

        initDateLayout(requireNotNull(mission.missionProgress))
    }

    private fun showRedeemableMission(mission: Mission) {
        title.isVisible = false
        join_layout.isVisible = false
        join_button.isVisible = false
        join_terms.isVisible = false
        join_layout_separator.isVisible = false
        how_to_redeem.isVisible = false

        quit_button.isVisible = false
        quit_button_separator.isVisible = false

        redeem_layout.isVisible = true
        redeem_later_button.isVisible = true
        redeem_button.isEnabled = true
        congrats_title_layout.isVisible = true

        initDateLayout(requireNotNull(mission.missionProgress))
    }

    private fun initDateLayout(progress: MissionProgress) {
        progress as MissionProgress.TypeDaily
        val dates = progress.toDates()
        require(dates.size <= 8) { "Mission must less or equal than 8 days" }
        dates.forEachIndexed { index, date ->
            initDateView(index, date.dateText, date.isCompleted)
        }
        if (dates.size <= 4) {
            date_layout_row_2.isVisible = false
        }
    }

    private fun initDateView(position: Int, dateText: String, active: Boolean) {
        val dateLayout = if (position < 4) {
            date_layout_row_1
        } else {
            date_layout_row_2
        }
        val relativePos = position % 4
        val dateView = dateLayout.getChildAt(relativePos)

        dateView.isVisible = true
        dateView.isActivated = active
        dateView.day_text.text = dateText
    }

    private fun observeActions() {
        missionDetailViewModel.showToast.observe(this, Observer {
            appContext().showToast(it)
        })
        missionDetailViewModel.startMissionReminder.observe(this, Observer { mission ->
            DailyMissionReminderWorker.startMissionReminder(appContext(), mission)
        })
        missionDetailViewModel.stopMissionReminder.observe(this, Observer { mission ->
            DailyMissionReminderWorker.stopMissionReminder(appContext(), mission)
        })
        missionDetailViewModel.closePage.observe(this, Observer {
            findNavController().popBackStack()
        })
        missionDetailViewModel.closeAllMissionPages.observe(this, Observer {
            requireActivity().finish()
        })
        missionDetailViewModel.requestFxLogin.observe(this, Observer { uid ->
            openFxLoginPage(uid)
        })
        missionDetailViewModel.openCouponPage.observe(this, Observer { mission ->
            openCouponPage(mission)
        })
    }

    override fun onNavigationResult(result: Bundle) {
        val jwt = result.getString(RESULT_STR_JWT)
        missionDetailViewModel.onFxLoginCompleted(jwt)
    }

    private fun openFxLoginPage(uid: String) {
        findNavController().navigate(MissionDetailFragmentDirections.actionMissionDetailDestToFxLoginDest(uid))
    }

    private fun openCouponPage(mission: Mission) {
        findNavController().navigate(MissionDetailFragmentDirections.actionMissionDetailDestToMissionCouponDest(mission))
    }

    data class DateUiModel(
        val dateText: String,
        val isCompleted: Boolean
    )

    companion object {
        const val RESULT_STR_JWT = "result_str_jwt"
    }
}

private fun MissionProgress.TypeDaily.toDates(): List<MissionDetailFragment.DateUiModel> {
    val results = mutableListOf<MissionDetailFragment.DateUiModel>()

    val sdf = SimpleDateFormat("MMM d", Locale.US)
    val calendarStart = Calendar.getInstance().apply {
        timeInMillis = joinDate
    }
    repeat(totalDays) { i ->
        val dateText = sdf.format(calendarStart.time)
        val isCompleted = (i + 1) <= currentDay
        results += MissionDetailFragment.DateUiModel(dateText, isCompleted)

        calendarStart.add(Calendar.DATE, 1)
    }

    return results
}