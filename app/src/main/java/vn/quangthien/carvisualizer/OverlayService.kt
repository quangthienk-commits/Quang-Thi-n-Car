package vn.quangthien.carvisualizer

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import android.graphics.drawable.GradientDrawable

/** Two separate windows: non-touchable translucent LED strip + draggable clickable bubble. */
class OverlayService: Service() {
    private lateinit var wm: WindowManager
    private var strip:View?=null
    private var bubble:View?=null
    private var started=false
    private val channel="qt_overlay"
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){super.onCreate();wm=getSystemService(WINDOW_SERVICE) as WindowManager;Config.load(this)}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="STOP"){stopSelf();return START_NOT_STICKY}
        if(started)return START_STICKY
        notification()
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        try {attach();started=true} catch(e:Exception){android.util.Log.e("QTOverlay","Overlay blocked",e);stopSelf()}
        return START_STICKY
    }
    private fun notification(){
        if(Build.VERSION.SDK_INT>=26){
            val ch=NotificationChannel(channel,"QUANG THIỆN RGB overlay",NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val open=PendingIntent.getActivity(this,1,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,2,Intent(this,OverlayService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n=Notification.Builder(this,channel).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("QUANG THIỆN RGB").setContentText("Đang hiển thị LED nổi · chạm để cài đặt").setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Dừng",stop).build()
        startForeground(1001,n)
    }
    private fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
    private fun lp(w:Int,h:Int,gravity:Int,flags:Int,alpha:Float=1f)=WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT).apply {this.gravity=gravity;this.alpha=alpha;format=PixelFormat.TRANSLUCENT}
    private fun attach(){
        strip=VisualizerView(this,false)
        // Android 12+ restricts touch-through from untrusted overlays. Alpha <= 0.8.
        val barLp=lp(-1,dp(100),Gravity.BOTTOM,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, .70f)
        wm.addView(strip,barLp)
        val dot=TextView(this).apply{
            text="QT\n◷";gravity=Gravity.CENTER;setTextColor(0xffe7faff.toInt());textSize=15f;typeface=Typeface.DEFAULT_BOLD
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff222b60.toInt(),0xff050c1d.toInt())).apply {shape=GradientDrawable.OVAL;setStroke(dp(2),0xff9757fb.toInt())}
            elevation=dp(8).toFloat()
        }
        val bubbleLp=lp(dp(66),dp(66),Gravity.TOP or Gravity.RIGHT,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN).apply{x=dp(18);y=dp(85)}
        var downX=0f;var downY=0f;var initialX=0;var initialY=0;var moving=false;var lastTap=0L
        dot.setOnTouchListener {_,ev->
            when(ev.actionMasked){
                MotionEvent.ACTION_DOWN->{downX=ev.rawX;downY=ev.rawY;initialX=bubbleLp.x;initialY=bubbleLp.y;moving=false;true}
                MotionEvent.ACTION_MOVE->{
                    if(kotlin.math.hypot(ev.rawX-downX,ev.rawY-downY)>dp(6))moving=true
                    if(moving){bubbleLp.x=initialX+(downX-ev.rawX).toInt();bubbleLp.y=initialY+(ev.rawY-downY).toInt();wm.updateViewLayout(dot,bubbleLp)}
                    true
                }
                MotionEvent.ACTION_UP->{
                    if(!moving){val now=System.currentTimeMillis();if(now-lastTap<420){lastTap=0L;toggleClock()}else lastTap=now};true
                }
                else->false
            }
        }
        bubble=dot;wm.addView(dot,bubbleLp)
    }
    private fun toggleClock(){
        val active=ClockActivity.current
        if(active!=null){active.runOnUiThread {active.finish()};return}
        try {startActivity(Intent(this,ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))}
        catch(e:Exception){android.util.Log.w("QTOverlay","Head unit blocked activity launch",e)}
    }
    override fun onDestroy(){
        try{strip?.let{wm.removeView(it)}}catch(_:Exception){}
        try{bubble?.let{wm.removeView(it)}}catch(_:Exception){}
        strip=null;bubble=null;started=false
        super.onDestroy()
    }
}
