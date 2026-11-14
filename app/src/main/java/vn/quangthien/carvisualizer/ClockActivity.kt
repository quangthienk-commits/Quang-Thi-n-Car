package vn.quangthien.carvisualizer

import android.app.Activity
import android.os.Bundle
import android.view.View

class ClockActivity: Activity() {
    companion object { @Volatile var current:ClockActivity?=null }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);Config.load(this);window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;setContentView(VisualizerView(this))}
    override fun onResume(){super.onResume();current=this}
    override fun onPause(){if(current===this)current=null;super.onPause()}
}
