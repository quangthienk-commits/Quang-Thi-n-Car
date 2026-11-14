package vn.quangthien.carvisualizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*

class MainActivity: Activity() {
    private lateinit var status: TextView
    private lateinit var preview: VisualizerView
    private val cyan=0xff52dcff.toInt()
    private fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
    private fun text(t:String,size:Float=14f):TextView = TextView(this).apply { text=t; textSize=size;setTextColor(0xffe8f4ff.toInt());setPadding(dp(8),dp(5),dp(8),dp(5)) }
    private fun button(label:String,action:()->Unit):Button=Button(this).apply { text=label;textSize=11f;setAllCaps(false);setOnClickListener { action() } }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        Config.load(this)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.navigationBarColor=Color.BLACK
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setBackgroundColor(0xff050a17.toInt())}
        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(8),0)}
        header.addView(text("◉ QUANG THIỆN  |  CAR RGB V0.1",17f),LinearLayout.LayoutParams(0,dp(41),1f))
        header.addView(button("ĐỒNG HỒ LỚN") { startActivity(Intent(this,ClockActivity::class.java)) })
        header.addView(button("BẬT NỔI") { turnOverlayOn() })
        header.addView(button("TẮT NỔI") { stopService(Intent(this,OverlayService::class.java)) })
        root.addView(header)
        preview=VisualizerView(this,true)
        root.addView(preview,LinearLayout.LayoutParams(-1,0,1.3f))
        status=text("Đang khởi động",12f)
        root.addView(status)
        val scroll=ScrollView(this).apply { isFillViewport=false }
        val controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(9),dp(4),dp(9),dp(15))}
        fun chooser(title:String,options:List<String>,value:Int,onSelect:(Int)->Unit){
            val row=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            row.addView(text(title,12f),LinearLayout.LayoutParams(dp(125),-2))
            val spin=Spinner(this)
            val adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
            spin.adapter=adapter;spin.setSelection(value,false)
            spin.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent:android.widget.AdapterView<*>?)=Unit
                override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long){onSelect(position);Config.save(this@MainActivity)}
            }
            row.addView(spin,LinearLayout.LayoutParams(0,dp(45),1f));controls.addView(row)
        }
        fun seek(title:String,value:Int,max:Int=100,onSelect:(Int)->Unit){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            val label=text(title,12f);row.addView(label,LinearLayout.LayoutParams(dp(125),-2))
            val sb=SeekBar(this).apply { this.max=max;progress=value }
            row.addView(sb,LinearLayout.LayoutParams(0,dp(41),1f))
            val number=text("$value",12f);row.addView(number,LinearLayout.LayoutParams(dp(48),-2))
            sb.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onStartTrackingTouch(seekBar:SeekBar?)=Unit
                override fun onStopTrackingTouch(seekBar:SeekBar?)=Unit
                override fun onProgressChanged(seekBar:SeekBar?,p:Int,fromUser:Boolean){if(fromUser){number.text="$p";onSelect(p);Config.save(this@MainActivity)}}
            });controls.addView(row)
        }
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        row.addView(button("BẮT ÂM THANH THẬT") { enableLive() })
        row.addView(button("DEMO") { Config.demo=true;Config.save(this);updateStatus() })
        row.addView(button("ÂM THANH THẬT") { Config.demo=false;Config.save(this);enableLive() })
        controls.addView(row)
        chooser("Hiệu ứng LED",Config.effects,Config.effect){Config.effect=it}
        chooser("Mẫu đồng hồ",Config.faces,Config.face){Config.face=it}
        chooser("Font QUANG THIỆN",Config.fonts,Config.font){Config.font=it}
        chooser("Màu RGB/chữ",Config.colors,Config.color){Config.color=it}
        controls.addView(CheckBox(this).apply {text="Hiển thị hai ngựa RGB tĩnh";setTextColor(Color.WHITE);isChecked=Config.horses;setOnCheckedChangeListener{_,checked->Config.horses=checked;Config.save(this@MainActivity)}})
        seek("Độ cao LED",Config.height){Config.height=it}
        seek("Độ rộng cột",Config.barWidth){Config.barWidth=it}
        seek("Độ sáng LED",Config.brightness){Config.brightness=it}
        seek("Glow",Config.glow){Config.glow=it}
        seek("Số cột",Config.count,130){Config.count=it.coerceAtLeast(24)}
        seek("Tốc độ",Config.speed){Config.speed=it}
        seek("BASS",Config.bass){Config.bass=it}
        seek("MID",Config.mid){Config.mid=it}
        seek("TREBLE",Config.treble){Config.treble=it}
        controls.addView(text("Nếu đang phát nhạc nhưng LED không nhận tín hiệu: đầu Android/ứng dụng phát có thể chặn audio session 0. Chế độ Demo sẽ luôn dùng mô phỏng, không phải âm thanh thực.",11f))
        scroll.addView(controls);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        updateStatus()
        if(!Config.demo)enableLive()
    }
    private fun updateStatus(){status.text=if(Config.demo) "● DEMO: hiệu ứng mô phỏng (không lấy âm thanh thật)" else if(AudioEngine.live) "● LIVE: đang phân tích đầu ra hệ thống" else "● LIVE CHƯA SẴN SÀNG: ${AudioEngine.error}"}
    private fun enableLive(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),101);return}
        Config.demo=false;Config.save(this);AudioEngine.start();updateStatus()
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==101 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) enableLive() else {Config.demo=true;Config.save(this);updateStatus()}
    }
    private fun turnOverlayOn(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            Toast.makeText(this,"Hãy cấp quyền hiển thị trên ứng dụng khác rồi quay lại bấm BẬT NỔI",Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));return
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),102)}
        val service=Intent(this,OverlayService::class.java)
        if(Build.VERSION.SDK_INT>=26)startForegroundService(service) else startService(service)
        Toast.makeText(this,"Đã yêu cầu bật thanh LED nổi + bong bóng QT",Toast.LENGTH_SHORT).show()
    }
    override fun onResume(){super.onResume();if(::status.isInitialized)updateStatus()}
}
