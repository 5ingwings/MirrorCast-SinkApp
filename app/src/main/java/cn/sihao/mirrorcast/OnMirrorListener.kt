package cn.sihao.mirrorcast

interface OnMirrorListener {
    fun onSessionBegin()

    fun onSessionEnd()

    fun onMirrorStart()

    fun onMirrorData(seqNum: Long, data: ByteArray?)

    fun onMirrorStop()
}