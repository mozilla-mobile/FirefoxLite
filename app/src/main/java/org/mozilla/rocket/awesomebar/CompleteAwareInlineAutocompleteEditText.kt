package org.mozilla.rocket.awesomebar

import android.content.Context
import android.util.AttributeSet
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R

open class CompleteAwareInlineAutocompleteEditText @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : InlineAutocompleteEditText(ctx, attrs, defStyleAttr) {

    var isUserInput = true

    override fun applyAutocompleteResult(result: AutocompleteResult) {

        isUserInput = result.text == text.toString() || result.totalItems == 0

        super.applyAutocompleteResult(result)
    }
}