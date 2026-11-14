package vn.quangthien.carvisualizer

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class VisualizerView(ctx:Context, private val fullClock:Boolean=true): View(ctx) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val leftHorse=BitmapFactory.decodeResource(resources,R.drawable.horse_left)
    private val rightHorse=BitmapFactory.decodeResource(resources,R.drawable.horse_right)
    private val labels=SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi","VN"))
    private val v=FloatArray(130)
    private var lastTime=System.nanoTime()
    private var time=0f
    init { setLayerType(LAYER_TYPE_SOFTWARE,null) }
    private fun color(hex:Long)=hex.toInt()
    private fun paint(c:Int,alpha:Int=255,style:Paint.Style=Paint.Style.FILL,stroke:Float=1f) {
        p.reset();p.isAntiAlias=true;p.color=c;p.alpha=alpha.coerceIn(0,255);p.style=style;p.strokeWidth=stroke;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND
    }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val w=width.toFloat();val h=height.toFloat()
        if(w<10||h<10)return
        val now=System.nanoTime();val dt=((now-lastTime)/1e9f).coerceIn(0f,.1f);lastTime=now;time+=dt*(.3f+Config.speed/70f)
        paint(Color.rgb(3,6,18));c.drawColor(p.color)
        p.shader=RadialGradient(w*.5f,h*.35f,w*.65f,intArrayOf(0xff172958.toInt(),0xff060d24.toInt(),Color.BLACK),null,Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h,p);p.shader=null
        if(fullClock){
            if(Config.horses) drawHorses(c,w,h)
            drawClock(c,w*.5f,h*.36f,min(h*.35f,w*.19f))
            paint(0xffb8cafa.toInt());p.textSize=h*.026f;p.textAlign=Paint.Align.CENTER
            c.drawText(labels.format(System.currentTimeMillis()),w*.5f,h*.685f,p)
            drawName(c,w*.5f,h*.79f,h)
        }
        drawBars(c,w,h)
        postInvalidateDelayed(32)
    }
    private fun drawHorses(c:Canvas,w:Float,h:Float){
        p.reset();p.isAntiAlias=true;p.isFilterBitmap=true
        p.alpha=235;p.colorFilter=null
        c.drawBitmap(leftHorse,null,RectF(w*.005f,h*.13f,w*.35f,h*.71f),p)
        c.drawBitmap(rightHorse,null,RectF(w*.65f,h*.13f,w*.995f,h*.71f),p)
    }
    private fun drawClock(c:Canvas,x:Float,y:Float,r:Float){
        val rim=when(Config.face){1->0xffff4e74.toInt();2->0xffedc777.toInt();3->0xff41f4ec.toInt();else->0xff42a8ff.toInt()}
        paint(rim,150,Paint.Style.STROKE, max(2f,r*.025f));p.setShadowLayer(r*.09f,0f,0f,rim);c.drawCircle(x,y,r,p);p.clearShadowLayer()
        paint(0xff0b152e.toInt());c.drawCircle(x,y,r*.965f,p)
        paint(0xff688cc1.toInt(),90,Paint.Style.STROKE,1.5f);c.drawCircle(x,y,r*.91f,p)
        for(i in 0 until 60){val a=i*PI.toFloat()/30f-PI.toFloat()/2f;val major=i%5==0
            val start=if(major).79f else .85f
            paint(if(major) Color.WHITE else 0xff7590b6.toInt(),if(major)240 else 120,Paint.Style.STROKE,if(major)r*.014f else r*.007f)
            c.drawLine(x+cos(a)*r*start,y+sin(a)*r*start,x+cos(a)*r*.9f,y+sin(a)*r*.9f,p)
        }
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD);p.textSize=r*.17f
        for((label,ang) in listOf("12" to -PI/2,"3" to 0.0,"6" to PI/2,"9" to PI)) {
            paint(Color.WHITE);p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD);p.textSize=r*.17f;p.textAlign=Paint.Align.CENTER
            val a=ang.toFloat();c.drawText(label,x+cos(a)*r*.66f,y+sin(a)*r*.66f+r*.055f,p)
        }
        paint(rim);p.textSize=r*.085f;p.typeface=Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.CENTER
        c.drawText("SPORT EDITION",x,y-r*.3f,p)
        val cal=Calendar.getInstance();val hours=cal.get(Calendar.HOUR);val minutes=cal.get(Calendar.MINUTE);val seconds=cal.get(Calendar.SECOND)
        hand(c,x,y,r*.47f,(hours+minutes/60f)*30f-90,Color.WHITE,r*.032f)
        hand(c,x,y,r*.65f,(minutes+seconds/60f)*6f-90,0xff4ee9ff.toInt(),r*.018f)
        hand(c,x,y,r*.75f,seconds*6f-90,0xffff4776.toInt(),r*.011f)
        paint(0xffd9faff.toInt());p.setShadowLayer(r*.08f,0f,0f,rim);c.drawCircle(x,y,r*.05f,p);p.clearShadowLayer()
    }
    private fun hand(c:Canvas,x:Float,y:Float,len:Float,degrees:Float,col:Int,thick:Float){val a=Math.toRadians(degrees.toDouble()).toFloat();paint(col,255,Paint.Style.STROKE,thick);c.drawLine(x,y,x+cos(a)*len,y+sin(a)*len,p)}
    private fun drawName(c:Canvas,x:Float,y:Float,h:Float){
        val fonts=arrayOf("cursive","casual","serif","serif","sans-serif-black")
        val variants=arrayOf(Typeface.BOLD_ITALIC,Typeface.BOLD_ITALIC,Typeface.BOLD_ITALIC,Typeface.BOLD,Typeface.BOLD)
        paint(if(Config.color==0)Color.WHITE else Config.palette(0,.5f))
        p.typeface=Typeface.create(fonts[Config.font.coerceIn(0,4)],variants[Config.font.coerceIn(0,4)])
        p.textSize=h*.071f;p.textAlign=Paint.Align.CENTER
        val title="QUANG THIỆN"
        if(Config.color==0){p.shader=LinearGradient(x-h*.35f,y,x+h*.35f,y,intArrayOf(0xff48f8e9.toInt(),0xffffdd47.toInt(),0xfff150c7.toInt(),0xff34caff.toInt()),null,Shader.TileMode.CLAMP)}
        p.setShadowLayer(12f,0f,0f,Config.palette(0,time%1f));c.drawText(title,x,y,p);p.clearShadowLayer();p.shader=null
    }
    private fun drawBars(c:Canvas,w:Float,h:Float){
        val n=Config.count.coerceIn(24,130);val bottom=h*.985f;val maxHeight=h*.21f*(Config.height/100f)
        val cell=w/n;val barW=min(cell*.93f,cell*(.15f+.8f*Config.barWidth/100f));val center=n/2f
        val usingDemo=Config.demo || !AudioEngine.live
        for(i in 0 until n){
            val x=(i+.5f)*cell;val distance=abs(i-center)/(center.coerceAtLeast(1f));val t=time;val pos=i.toFloat()/n
            val band=if(pos<.33f)Config.bass else if(pos<.7f)Config.mid else Config.treble
            val amp=if(usingDemo) (.2f+.8f*abs(sin(t*(if(pos<.33f) 5 else if(pos<.7f) 9 else 15)+i*.28f))) *band/100f
                else AudioEngine.spectrum((pos*127).toInt())*band/100f
            val pulse=if(usingDemo) (.5f+.5f*sin(t*5f)) else AudioEngine.bassLevel
            val target=when(Config.effect){
                0 -> .08f+.86f*max(0f,sin(t*4-distance*15)).pow(2)*(.2f+.8f*amp)
                1 -> .08f+.88f*max(0f,cos(t*7-distance*24)).pow(4)*(.2f+.8f*amp)
                2 -> .07f+.9f*amp
                3 -> .07f+.87f*abs(sin(distance*12-t*3))*(.2f+.8f*amp)
                4 -> .08f+.85f*abs(sin(pos*12-t*2))*(.3f+.7f*amp)
                5 -> .06f+.9f*max(0f,sin(t*5-distance*20)).pow(5)
                6 -> .05f+.95f*pulse.pow(3)
                7 -> .05f+.95f*max(0f,sin(t*8-distance*13)).pow(10)
                8 -> .2f+.3f*sin(pos*12-t*2)+.3f*amp
                9 -> .05f+.92f*max(0f,sin(pos*25-t*6)).pow(5)
                10 -> (amp*10).toInt()/10f
                else -> .07f+.91f*abs(sin(t*8+i*.33f)).pow(2)*amp
            }.coerceIn(.03f,1f)
            v[i]+=(target-v[i])*.25f
            val heightBar=max(2f,maxHeight*v[i]);val col=Config.palette(i,pos)
            paint(col,((Config.brightness/100f)*255).toInt())
            p.setShadowLayer(Config.glow/5f,0f,0f,col)
            c.drawRoundRect(x-barW/2,bottom-heightBar,x+barW/2,bottom,barW*.2f,barW*.2f,p)
            p.clearShadowLayer()
            paint(0xff020510.toInt(),110,Paint.Style.STROKE,max(1f,h*.0014f))
            var yy=bottom-7f;while(yy>bottom-heightBar+2f){c.drawLine(x-barW/2,yy,x+barW/2,yy,p);yy-=7f}
        }
        if(Config.effect in listOf(0,1,5,7)) {
            val strength=if(usingDemo) .4f+.6f*abs(sin(time*3)) else AudioEngine.bassLevel
            val cx=w/2f;val cy=bottom;val rr=w*(.025f+strength*.1f)
            paint(0xff4de9ff.toInt(),(strength*185).toInt(),Paint.Style.STROKE,max(1f,w*.0015f))
            p.setShadowLayer(15f,0f,0f,0xff6f4eff.toInt());c.drawOval(cx-rr,cy-rr*.18f,cx+rr,cy+rr*.18f,p);p.clearShadowLayer()
        }
    }
}
