package cn.sihao.mirrorcast

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.orhanobut.logger.Logger


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
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
