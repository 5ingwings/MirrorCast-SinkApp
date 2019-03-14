package cn.sihao.mirrorcast

import android.app.Application
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class MiraApplication: Application() {
    companion object {

        private val TAG = "MiraApplication"
    }

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init(){
        Logger.addLogAdapter(AndroidLogAdapter())
    }
}