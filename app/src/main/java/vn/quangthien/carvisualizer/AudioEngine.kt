package vn.quangthien.carvisualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.*

/** Reads Android output mix (session 0). Some vendors restrict this and some sources are not in the mix. */
object AudioEngine {
    @Volatile var live = false
        private set
    @Volatile var error = "Chưa bật thu âm"
        private set
    @Volatile var bassLevel = 0f
    @Volatile var midLevel = 0f
    @Volatile var trebleLevel = 0f
    private var visualizer: Visualizer? = null
    private val spectral = FloatArray(128)
    private val guard = Any()

    fun start(): Boolean = synchronized(guard) {
        stop()
        try {
            val v = Visualizer(0)
            v.enabled = false
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = min(1024, range[1]).coerceAtLeast(range[0])
            v.setDataCaptureListener(object: Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if(fft == null || fft.size < 4) return
                    val bins=fft.size/2
                    // Sampling rate from Android Visualizer is in milli-Hz.
                    val sampleHz=samplingRate/1000f
                    val binHz=sampleHz/fft.size
                    var b=0f;var m=0f;var t=0f;var bc=0;var mc=0;var tc=0
                    for(k in 1 until bins) {
                        val re=fft[2*k].toInt().toFloat();val im=fft[2*k+1].toInt().toFloat()
                        val magnitude=sqrt(re*re+im*im)/180f
                        val freq=k*binHz
                        when {
                            freq in 35f..250f->{b+=magnitude;bc++}
                            freq in 250f..4000f->{m+=magnitude;mc++}
                            freq in 4000f..16000f->{t+=magnitude;tc++}
                        }
                    }
                    fun normalize(sum:Float,n:Int)=if(n>0) (sqrt(sum/n)*1.6f).coerceIn(0f,1f) else 0f
                    bassLevel=bassLevel*.65f+normalize(b,bc)*.35f
                    midLevel=midLevel*.65f+normalize(m,mc)*.35f
                    trebleLevel=trebleLevel*.65f+normalize(t,tc)*.35f
                    synchronized(spectral) {
                        for(i in spectral.indices) {
                            val k=(1f+i.toFloat()/spectral.size*(bins-2)).toInt().coerceIn(1,bins-1)
                            val re=fft[2*k].toInt().toFloat();val im=fft[2*k+1].toInt().toFloat()
                            val v=(sqrt(re*re+im*im)/100f).coerceIn(0f,1f)
                            spectral[i]=spectral[i]*.55f+v*.45f
                        }
                    }
                }
            },Visualizer.getMaxCaptureRate()/2,false,true)
            v.enabled=true
            visualizer=v
            live=true;error="Đang đọc âm thanh hệ thống"
            true
        } catch (e: Exception) {
            live=false;error="Đầu Android không hỗ trợ: ${e.javaClass.simpleName}"
            Log.w("QTAudio",error,e)
            false
        }
    }
    fun spectrum(index: Int):Float = synchronized(spectral) { spectral[index.coerceIn(0,spectral.lastIndex)] }
    fun stop() {
        try {visualizer?.enabled=false;visualizer?.release()} catch(_: Exception) {}
        visualizer=null;live=false
    }
}
