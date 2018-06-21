package org.mozilla.focus.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_edit_bookmark.*
import org.mozilla.focus.R

private const val SAVE_ACTION_ID = 1;

class EditBookmarkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bookmark)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val edit = menu.add(Menu.NONE, SAVE_ACTION_ID, Menu.NONE, R.string.bookmark_edit_save)
        edit.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            SAVE_ACTION_ID -> Toast.makeText(this, R.string.bookmark_edit_success, Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

}
