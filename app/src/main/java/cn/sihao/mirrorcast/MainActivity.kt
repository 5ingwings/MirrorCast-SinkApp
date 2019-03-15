package cn.sihao.mirrorcast

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.orhanobut.logger.Logger


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.systemUiVisibility = option
        window.statusBarColor = Color.TRANSPARENT
        // 防息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        startMiraCast()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMiraCast()
    }


    private fun startMiraCast() {
        MiraMgr.instance.start(applicationContext)
    }

    private fun stopMiraCast() {
        MiraMgr.instance.stop()
    }

}
