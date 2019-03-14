package cn.sihao.mirrorcast

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startMiraCast()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMiraCast()
    }


    private fun startMiraCast(){
        MiraMgr.instance.start(applicationContext)
    }

    private fun stopMiraCast(){
        MiraMgr.instance.stop()
    }

}
