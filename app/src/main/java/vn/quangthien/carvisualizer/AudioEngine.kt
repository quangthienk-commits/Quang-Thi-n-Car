package vn.quangthien.carvisualizer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.*

/**
 * Reads Android's output mix when available, or listens to the car speakers via microphone.
 * No live data is ever replaced with simulated data without selecting Demo explicitly.
 * Audio samples are processed in memory only: there is no recording file or network use.
 */
object AudioEngine {
    @Volatile var live=false
        private set
    @Volatile var mode=0
        private set
    @Volatile var error="Chưa chọn nguồn âm thanh"
        private set
    @Volatile var bassLevel=0f
        private set
    @Volatile var midLevel=0f
        private set
    @Volatile var trebleLevel=0f
        private set
    @Volatile var signal=0f
        private set
    @Volatile var lastCaptureAt=0L
        private set
    private var visualizer: Visualizer?=null
    private var recorder: AudioRecord?=null
    private var thread: Thread?=null
    private var recording=false
    private val spectral=FloatArray(128)
    private val guard=Any()

    fun start(context: Context, requestedMode:Int): Boolean = synchronized(guard) {
        if(live && mode==requestedMode) return@synchronized true
        stop()
        mode=requestedMode
        if(requestedMode==0){error="Demo: tín hiệu mô phỏng";return@synchronized true}
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            error="Chưa cấp quyền micro/RECORD_AUDIO";return@synchronized false
        }
        return@synchronized if(requestedMode==1) startSystem() else startMicrophone()
    }
    private fun startSystem():Boolean {
        try {
            val v=Visualizer(0)
            visualizer=v
            v.enabled=false
            val range=Visualizer.getCaptureSizeRange()
            v.captureSize=1024.coerceIn(range[0],range[1])
            v.setDataCaptureListener(object:Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer:Visualizer?,waveform:ByteArray?,samplingRate:Int)=Unit
                override fun onFftDataCapture(visualizer:Visualizer?,fft:ByteArray?,samplingRate:Int){
                    if(fft==null||fft.size<8)return
                    val n=fft.size/2;val hz=samplingRate/1000f;val binHz=hz/fft.size
                    val mags=FloatArray(n)
                    for(k in 1 until n){val re=fft[k*2].toFloat();val im=fft[k*2+1].toFloat();mags[k]=sqrt(re*re+im*im)/128f}
                    updateSpectrum(mags,binHz)
                }
            },Visualizer.getMaxCaptureRate()/2,false,true)
            v.enabled=true
            visualizer=v;live=true;error="Đang thử nhận âm thanh hệ thống";return true
        } catch(e:Exception) {
            error="Không mở được Visualizer hệ thống: ${e.javaClass.simpleName}: ${e.message ?: ""}"
            Log.w("QTAudio",error,e);try{visualizer?.release()}catch(_:Exception){};visualizer=null
            return false
        }
    }
    private fun startMicrophone():Boolean {
        try {
            val rate=22050;val size=1024
            val minBuf=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
            require(minBuf>0){"Thiết bị không hỗ trợ AudioRecord"}
            val ar=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,max(minBuf,size*4))
            require(ar.state==AudioRecord.STATE_INITIALIZED){"Không khởi tạo được microphone"}
            ar.startRecording()
            recorder=ar;recording=true;live=true;error="Micro đang nghe âm thanh bên ngoài"
            thread=Thread({
                val pcm=ShortArray(size)
                val real=FloatArray(size);val imag=FloatArray(size)
                try {
                    while(recording && !Thread.currentThread().isInterrupted){
                        val read=ar.read(pcm,0,size,AudioRecord.READ_BLOCKING)
                        if(read<0){error="Lỗi đọc Micro: $read";live=false;break}
                        if(read<size)continue
                        for(i in 0 until size){real[i]=pcm[i]/32768f*(.5f-.5f*cos(2f*PI.toFloat()*i/(size-1)));imag[i]=0f}
                        fft(real,imag)
                        val mags=FloatArray(size/2)
                        for(k in 1 until size/2)mags[k]=(hypot(real[k],imag[k])/14f).coerceIn(0f,3f)
                        updateSpectrum(mags,rate.toFloat()/size)
                    }
                } catch(e:Exception){if(recording){error="Micro bị ngắt: ${e.message}";live=false;Log.w("QTAudio",error,e)}}
            },"QT-Audio-Micro").apply {isDaemon=true;start()}
            return true
        } catch(e:Exception){
            error="Không bật được Micro: ${e.javaClass.simpleName}: ${e.message ?: ""}"
            Log.w("QTAudio",error,e)
            recorder?.release();recorder=null;live=false;recording=false
            return false
        }
    }
    private fun fft(real:FloatArray, imag:FloatArray){
        val n=real.size;var j=0
        for(i in 1 until n){var bit=n shr 1;while(j and bit!=0){j=j xor bit;bit=bit shr 1};j=j xor bit
            if(i<j){val r=real[i];real[i]=real[j];real[j]=r;val z=imag[i];imag[i]=imag[j];imag[j]=z}}
        var len=2
        while(len<=n){val angle=-2.0*PI/len;val wrStep=cos(angle).toFloat();val wiStep=sin(angle).toFloat()
            for(start in 0 until n step len){var wr=1f;var wi=0f
                for(k in 0 until len/2){val a=start+k;val b=a+len/2;val rr=real[b]*wr-imag[b]*wi;val ii=real[b]*wi+imag[b]*wr
                    real[b]=real[a]-rr;imag[b]=imag[a]-ii;real[a]+=rr;imag[a]+=ii
                    val nr=wr*wrStep-wi*wiStep;wi=wr*wiStep+wi*wrStep;wr=nr}}
            len=len shl 1
        }
    }
    private fun updateSpectrum(mags:FloatArray,binHz:Float){
        if(binHz<=0f)return
        var b=0f;var m=0f;var t=0f;var bc=0;var mc=0;var tc=0
        for(k in 1 until mags.size){val f=k*binHz;val v=mags[k]
            when {f<250f && f>=35f->{b+=v;bc++};f<4000f && f>=250f->{m+=v;mc++};f<=11000f && f>=4000f->{t+=v;tc++}}
        }
        fun norm(sum:Float,n:Int):Float=if(n==0)0f else (sqrt(sum/n)*.95f).coerceIn(0f,1f)
        bassLevel=.6f*bassLevel+.4f*norm(b,bc)
        midLevel=.6f*midLevel+.4f*norm(m,mc)
        trebleLevel=.6f*trebleLevel+.4f*norm(t,tc)
        signal=(bassLevel*.45f+midLevel*.35f+trebleLevel*.2f).coerceIn(0f,1f)
        synchronized(spectral){for(i in spectral.indices){
            val ratio=i/(spectral.size-1f)
            val freq=35f*(11000f/35f).pow(ratio)
            val index=(freq/binHz).toInt().coerceIn(1,mags.lastIndex)
            spectral[i]=(spectral[i]*.55f+min(1f,sqrt(mags[index])*.9f)*.45f)
        }}
        lastCaptureAt=System.currentTimeMillis()
        if(mode==1)error=if(signal<.025f)"Visualizer đã mở, nhưng chưa nhận tín hiệu (hãy phát nhạc)" else "Đang nhận tín hiệu hệ thống"
    }
    fun spectrum(index:Int):Float=synchronized(spectral){spectral[index.coerceIn(0,spectral.lastIndex)]}
    fun stop() {
        recording=false
        try{recorder?.stop()}catch(_:Exception){}
        try{recorder?.release()}catch(_:Exception){}
        recorder=null;thread?.interrupt();thread=null
        try{visualizer?.enabled=false;visualizer?.release()}catch(_:Exception){}
        visualizer=null;live=false;bassLevel=0f;midLevel=0f;trebleLevel=0f;signal=0f;lastCaptureAt=0L
        synchronized(spectral){spectral.fill(0f)}
    }
}
