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
    private val name: EditText by lazy { findViewById<EditText>(R.id.bookmark_name) }
    private val location: EditText by lazy { findViewById<EditText>(R.id.bookmark_location) }
    private val originalName: String by lazy { name.text.toString() }
    private val originalLocation: String by lazy { location.text.toString() }
    private lateinit var save: MenuItem
    private var nameChanged: Boolean = false
    private var locationChanged: Boolean = false
    private var locationEmpty: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bookmark)
        setSupportActionBar(toolbar)
        val drawable:Drawable = DrawableCompat.wrap(resources.getDrawable(R.drawable.edit_close, theme));
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.sharedColorAppPaletteWhite))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(drawable)
        name.setText(bookmark.name)
        location.setText(bookmark.address)
        name.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                nameChanged = s.toString() != originalName
                save.isEnabled = saveValid()
            }
        })
        location.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                locationChanged = s.toString() != originalLocation
                locationEmpty = TextUtils.isEmpty(s)
                save.isEnabled = saveValid()
            }
        })
        // Lazy init
        originalName
        originalLocation
    }

    fun saveValid(): Boolean {
        return !locationEmpty && (nameChanged || locationChanged)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        save = menu.add(Menu.NONE, SAVE_ACTION_ID, Menu.NONE, R.string.bookmark_edit_save)
        save.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        save.isEnabled = false
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
