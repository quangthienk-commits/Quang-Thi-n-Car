package vn.quangthien.carvisualizer

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import kotlin.math.hypot

/** Transparent, touch-through LED strip + separately touchable floating analog clock. */
class OverlayService: Service() {
    private lateinit var wm:WindowManager
    private var strip:View?=null
    private var bubble:View?=null
    private var started=false
    private val channel="qt_overlay"
    private val handler=Handler(Looper.getMainLooper())
    private val visibilityPoll=object:Runnable {override fun run(){bubble?.visibility=if(ClockActivity.current!=null) View.GONE else View.VISIBLE;handler.postDelayed(this,400)}}
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){super.onCreate();wm=getSystemService(WINDOW_SERVICE) as WindowManager;Config.load(this)}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="STOP"){stopSelf();return START_NOT_STICKY}
        if(started){
            Config.load(this)
            if(Config.audioMode!=AudioEngine.mode || (Config.audioMode!=0 && !AudioEngine.live)){
                try{notification();AudioEngine.start(this,Config.audioMode)}
                catch(e:Exception){Log.w("QTOverlay","Cannot switch audio mode",e)}
            }
            bubble?.invalidate();strip?.invalidate();return START_STICKY
        }
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        try{
            notification()
            if(Config.audioMode!=0)AudioEngine.start(this,Config.audioMode)
            attach();started=true;handler.post(visibilityPoll)
        }catch(e:Exception){Log.e("QTOverlay","Unable to start",e);stopSelf()}
        return START_STICKY
    }
    private fun notification(){
        if(Build.VERSION.SDK_INT>=26){
            val ch=NotificationChannel(channel,"QUANG THIỆN RGB overlay",NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val open=PendingIntent.getActivity(this,1,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,2,Intent(this,OverlayService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,channel) else Notification.Builder(this)
        val n=builder.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("QUANG THIỆN RGB")
            .setContentText("LED trong suốt · đồng hồ kim nổi · chọn để cài đặt")
            .setContentIntent(open).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Dừng",stop).build()
        if(Build.VERSION.SDK_INT>=29){
            var types=if(Build.VERSION.SDK_INT>=34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            if(Config.audioMode==2) types=types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(1001,n,types)
        }else startForeground(1001,n)
    }
    private fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
    private fun lp(w:Int,h:Int,gravity:Int,flags:Int,alpha:Float=1f)=WindowManager.LayoutParams(
        w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT
    ).apply {this.gravity=gravity;this.alpha=alpha;format=PixelFormat.TRANSLUCENT}
    private fun attach(){
        // This view paints no background and cannot consume touches.
        val led=VisualizerView(this,false)
        val ledLp=lp(-1,dp(120),Gravity.BOTTOM,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            .78f)
        wm.addView(led,ledLp);strip=led
        val clock=ClockBubbleView(this).apply{setLayerType(View.LAYER_TYPE_SOFTWARE,null);elevation=dp(10).toFloat()}
        val bubbleLp=lp(dp(104),dp(104),Gravity.TOP or Gravity.RIGHT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        ).apply{x=dp(20);y=dp(76)}
        var downX=0f;var downY=0f;var initialX=0;var initialY=0;var moving=false;var lastTap=0L
        clock.setOnTouchListener {_,ev->
            when(ev.actionMasked){
                MotionEvent.ACTION_DOWN->{downX=ev.rawX;downY=ev.rawY;initialX=bubbleLp.x;initialY=bubbleLp.y;moving=false;true}
                MotionEvent.ACTION_MOVE->{
                    if(hypot(ev.rawX-downX,ev.rawY-downY)>dp(6))moving=true
                    if(moving){
                        bubbleLp.x=initialX+(downX-ev.rawX).toInt()
                        bubbleLp.y=initialY+(ev.rawY-downY).toInt()
                        wm.updateViewLayout(clock,bubbleLp)
                    };true
                }
                MotionEvent.ACTION_UP->{
                    if(!moving){val now=System.currentTimeMillis();if(now-lastTap<450){lastTap=0L;toggleClock()}else lastTap=now};true
                }
                else->false
            }
        }
        wm.addView(clock,bubbleLp);bubble=clock
    }
    private fun toggleClock(){
        val active=ClockActivity.current
        if(active!=null){active.runOnUiThread{active.finish()};return}
        try{startActivity(Intent(this,ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))}
        catch(e:Exception){Log.w("QTOverlay","Firmware might block starting activity from background",e)}
    }
    override fun onDestroy(){
        handler.removeCallbacks(visibilityPoll)
        try{strip?.let{wm.removeView(it)}}catch(_:Exception){}
        try{bubble?.let{wm.removeView(it)}}catch(_:Exception){}
        strip=null;bubble=null;started=false;AudioEngine.stop()
        super.onDestroy()
    }
}
