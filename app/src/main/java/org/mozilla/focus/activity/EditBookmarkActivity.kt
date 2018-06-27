package org.mozilla.focus.activity

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_edit_bookmark.*

import org.mozilla.focus.R
import org.mozilla.rocket.temp.TempInMemoryBookmarkRepository
import java.util.UUID

private const val SAVE_ACTION_ID = 1;
const val ITEM_UUID_KEY = "ITEM_UUID_KEY"

class EditBookmarkActivity : AppCompatActivity() {

    private val itemId: UUID by lazy { intent.getSerializableExtra(ITEM_UUID_KEY) as UUID }
    private val bookmark: TempInMemoryBookmarkRepository.Bookmark by lazy { TempInMemoryBookmarkRepository.getInstance().get(itemId) }
    private val editTextName: EditText by lazy { findViewById<EditText>(R.id.bookmark_name) }
    private val editTextLocation: EditText by lazy { findViewById<EditText>(R.id.bookmark_location) }
    private val originalName: String by lazy { bookmark.name }
    private val originalLocation: String by lazy { bookmark.address }
    private lateinit var menuItemSave: MenuItem
    private var nameChanged: Boolean = false
    private var locationChanged: Boolean = false
    private var locationEmpty: Boolean = false
    private val nameWatcher: TextWatcher = object: TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            nameChanged = s.toString() != originalName
            setupMenuItemSave()
        }
    }
    private val locationWatcher: TextWatcher = object: TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            locationChanged = s.toString() != originalLocation
            locationEmpty = TextUtils.isEmpty(s)
            setupMenuItemSave()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bookmark)
        setSupportActionBar(toolbar)
        val drawable:Drawable = DrawableCompat.wrap(resources.getDrawable(R.drawable.edit_close, theme));
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.sharedColorAppPaletteWhite))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(drawable)
        editTextName.setText(bookmark.name)
        editTextLocation.setText(bookmark.address)
        editTextName.addTextChangedListener(nameWatcher)
        editTextLocation.addTextChangedListener(locationWatcher)
    }

    override fun onDestroy() {
        editTextName.removeTextChangedListener(nameWatcher)
        editTextLocation.removeTextChangedListener(locationWatcher)
        super.onDestroy()
    }

    private fun isSaveValid(): Boolean {
        return !locationEmpty && (nameChanged || locationChanged)
    }

    fun setupMenuItemSave() {
        if (::menuItemSave.isInitialized) {
            menuItemSave.isEnabled = isSaveValid()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuItemSave = menu.add(Menu.NONE, SAVE_ACTION_ID, Menu.NONE, R.string.bookmark_edit_save)
        menuItemSave.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        setupMenuItemSave()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            SAVE_ACTION_ID -> Toast.makeText(this, R.string.bookmark_edit_success, Toast.LENGTH_LONG).show()
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

}
