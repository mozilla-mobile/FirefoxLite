package org.mozilla.rocket.content.ecommerce.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_rating.view.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.DrawableUtils

class RatingView : LinearLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val ratingStar: Drawable?
    private val ratingBlankStar: Drawable?

    init {
        LayoutInflater.from(context).inflate(R.layout.view_rating, this, true)

        ratingStar = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_rating, ContextCompat.getColor(context, R.color.paletteDarkBlueC100))
        ratingBlankStar = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_rating_blank, ContextCompat.getColor(context, R.color.paletteDarkBlueC100))
    }

    fun updateRatingInfo(rating: Int, reviews: String) {
        rating_star_1.setImageDrawable(if (rating >= 1) ratingStar else ratingBlankStar)
        rating_star_2.setImageDrawable(if (rating >= 2) ratingStar else ratingBlankStar)
        rating_star_3.setImageDrawable(if (rating >= 3) ratingStar else ratingBlankStar)
        rating_star_4.setImageDrawable(if (rating >= 4) ratingStar else ratingBlankStar)
        rating_star_5.setImageDrawable(if (rating == 5) ratingStar else ratingBlankStar)
        rating_reviews.text = reviews
    }
}