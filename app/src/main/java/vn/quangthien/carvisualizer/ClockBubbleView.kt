package vn.quangthien.carvisualizer

import android.content.Context
import android.graphics.*
import android.view.View
import java.util.Calendar
import kotlin.math.*

/** Floating actual analog clock, no QT text. All drawing stays inside the circular touch target. */
class ClockBubbleView(ctx:Context):View(ctx){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private fun stroke(c:Int,width:Float){p.reset();p.isAntiAlias=true;p.color=c;p.style=Paint.Style.STROKE;p.strokeWidth=width;p.strokeCap=Paint.Cap.ROUND}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val x=width/2f;val y=height/2f;val r=min(width,height)*.47f
        p.reset();p.isAntiAlias=true;p.color=0xff071226.toInt();p.style=Paint.Style.FILL
        canvas.drawCircle(x,y,r,p)
        stroke(0xff9973ff.toInt(),r*.055f);p.setShadowLayer(r*.15f,0f,0f,0xff7d5cff.toInt());canvas.drawCircle(x,y,r*.95f,p);p.clearShadowLayer()
        stroke(0xff2ddfff.toInt(),r*.018f);canvas.drawCircle(x,y,r*.85f,p)
        for(i in 0..11){val a=i*PI.toFloat()/6f-PI.toFloat()/2
            stroke(if(i%3==0)Color.WHITE else 0xff8ab5ee.toInt(), if(i%3==0) r*.05f else r*.025f)
            canvas.drawLine(x+cos(a)*r*.67f,y+sin(a)*r*.67f,x+cos(a)*r*.77f,y+sin(a)*r*.77f,p)
        }
        val d=Calendar.getInstance();val h=d.get(Calendar.HOUR);val m=d.get(Calendar.MINUTE);val s=d.get(Calendar.SECOND)
        fun hand(angle:Float,length:Float,color:Int,thick:Float){val a=Math.toRadians((angle-90).toDouble()).toFloat();stroke(color,thick);canvas.drawLine(x,y,x+cos(a)*length,y+sin(a)*length,p)}
        hand((h+m/60f)*30f,r*.43f,Color.WHITE,r*.075f)
        hand((m+s/60f)*6f,r*.64f,0xff55eaff.toInt(),r*.045f)
        hand(s*6f,r*.72f,0xffff547a.toInt(),r*.024f)
        p.reset();p.isAntiAlias=true;p.color=Color.WHITE;canvas.drawCircle(x,y,r*.075f,p)
        postInvalidateDelayed(500)
    }
}
