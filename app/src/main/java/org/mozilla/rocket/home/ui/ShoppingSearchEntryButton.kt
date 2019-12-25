package org.mozilla.rocket.home.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.mozilla.focus.R
import org.mozilla.rocket.nightmode.themed.ThemedImageView

class ShoppingSearchEntryButton : ConstraintLayout {
    private val shoppingSearch: ThemedImageView
    private val ongoinIndicator: ImageView

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.button_shopping_search_entry, this)

        shoppingSearch = findViewById(R.id.shopping_search_icon)
        ongoinIndicator = findViewById(R.id.shopping_search_ongoing_indicator)
    }

    fun setOngoingState(state: Boolean) {
        ongoinIndicator.isVisible = state
    }

    fun setNightMode(isNight: Boolean) {
        shoppingSearch.setNightMode(isNight)
    }
}