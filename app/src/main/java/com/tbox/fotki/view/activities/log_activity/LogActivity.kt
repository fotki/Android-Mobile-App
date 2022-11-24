package com.tbox.fotki.view.activities.log_activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.tbox.fotki.R
import com.tbox.fotki.databinding.ActivityLogBinding
import com.tbox.fotki.util.destroyBroadcasts
import com.tbox.fotki.util.registerBroadcasts

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private lateinit var viewModel: LogActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_log)
        viewModel = ViewModelProviders.of(this).get(LogActivityViewModel::class.java)
        binding.viewModel = viewModel
        viewModel.loadText(this)
        registerBroadcasts(viewModel.broadcasts)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyBroadcasts(viewModel.broadcasts)
        //LogProvider.clearFIle(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.log_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.nav_send -> {
                viewModel.sendText(this)
                true
            }
            R.id.nav_clear -> {
                viewModel.clearText(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
