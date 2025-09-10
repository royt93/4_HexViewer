package com.galaxyjoy.hexviewer.ui.act

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.galaxyjoy.hexviewer.R
import com.galaxyjoy.hexviewer.sdkadbmob.UIUtils

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        setContentView(R.layout.activity_about)
        UIUtils.setupEdgeToEdge2(rootView = findViewById(R.id.layoutRoot), paddingTop = true, paddingBottom = false)
        supportActionBar?.apply {
            title = "About"
            setDisplayHomeAsUpEnabled(true)
        }

        val sourceCodeLink: TextView = findViewById(R.id.sourceCodeLink)
        sourceCodeLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/royt93/4_HexViewer"))
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}