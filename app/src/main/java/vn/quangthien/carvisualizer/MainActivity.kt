package vn.quangthien.carvisualizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.*

class MainActivity:Activity(){
    private lateinit var status:TextView
    private lateinit var preview:VisualizerView
    private var pendingOverlay=false
    private val handler=Handler(Looper.getMainLooper())
    private val refresh=object:Runnable{override fun run(){if(::status.isInitialized)updateStatus();handler.postDelayed(this,550)}}
    private fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
    private fun text(t:String,size:Float=14f):TextView=TextView(this).apply{text=t;textSize=size;setTextColor(0xffe8f4ff.toInt());setPadding(dp(8),dp(5),dp(8),dp(5))}
    private fun button(label:String,action:()->Unit):Button=Button(this).apply{text=label;textSize=11f;isAllCaps=false;setOnClickListener{action()}}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);Config.load(this)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.navigationBarColor=Color.BLACK
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xff050a17.toInt())}
        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(8),0)}
        header.addView(text("◉ CAR RGB V0.3",17f),LinearLayout.LayoutParams(0,dp(43),1f))
        header.addView(button("ĐỒNG HỒ LỚN"){startActivity(Intent(this,ClockActivity::class.java))})
        header.addView(button("BẬT NỔI"){turnOverlayOn()})
        header.addView(button("TẮT NỔI"){stopService(Intent(this,OverlayService::class.java))})
        root.addView(header)
        preview=VisualizerView(this,true)
        root.addView(preview,LinearLayout.LayoutParams(-1,0,1.2f))
        status=text("Khởi động...",12f);root.addView(status)
        val scroll=ScrollView(this)
        val controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(9),dp(4),dp(9),dp(16))}
        fun chooser(title:String,options:List<String>,value:Int,onSelect:(Int)->Unit){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            row.addView(text(title,12f),LinearLayout.LayoutParams(dp(148),-2))
            val spin=Spinner(this)
            spin.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
            spin.setSelection(value,false)
            spin.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent:AdapterView<*>?)=Unit
                override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){onSelect(position);Config.save(this@MainActivity)}
            }
            row.addView(spin,LinearLayout.LayoutParams(0,dp(45),1f));controls.addView(row)
        }
        fun seek(title:String,value:Int,max:Int=100,onSelect:(Int)->Unit){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            row.addView(text(title,12f),LinearLayout.LayoutParams(dp(148),-2))
            val sb=SeekBar(this).apply{this.max=max;progress=value}
            row.addView(sb,LinearLayout.LayoutParams(0,dp(41),1f))
            val number=text("$value",12f);row.addView(number,LinearLayout.LayoutParams(dp(44),-2))
            sb.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onStartTrackingTouch(seekBar:SeekBar?)=Unit
                override fun onStopTrackingTouch(seekBar:SeekBar?)=Unit
                override fun onProgressChanged(seekBar:SeekBar?,p:Int,fromUser:Boolean){if(fromUser){number.text="$p";onSelect(p);Config.save(this@MainActivity)}}
            });controls.addView(row)
        }
        controls.addView(text("NGUỒN NHẠC (kiểm tra nguồn theo trạng thái tín hiệu)",14f))
        chooser("Nguồn âm thanh",Config.audioModes,Config.audioMode){
            val changed=Config.audioMode!=it
            Config.audioMode=it
            Config.save(this@MainActivity)
            if(changed){AudioEngine.stop();if(it!=0)ensurePermissionAndStart() else updateStatus()}
        }
        val actions=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        actions.addView(button("THỬ LẠI NGUỒN NHẠC"){ensurePermissionAndStart()})
        actions.addView(button("KIỂM TRA DEMO"){Config.audioMode=0;AudioEngine.stop();Config.save(this);updateStatus()})
        controls.addView(actions)
        controls.addView(text("Nếu System không nhận nhạc, chọn Micro để thu âm thanh phát ra từ loa. Micro cần được cấp quyền và có thể thu cả âm thanh trong xe. Không ghi âm ra tệp và không gửi qua mạng.",11f))
        controls.addView(text("TÊN TRÊN MÀN HÌNH ĐỒNG HỒ",14f))
        val nameField=EditText(this).apply{setSingleLine(true);setText(Config.title);hint="Nhập tên tùy thích";setTextColor(Color.WHITE);setHintTextColor(0xff8291ae.toInt());textSize=16f;filters=arrayOf(android.text.InputFilter.LengthFilter(32))}
        val nameRow=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        nameRow.addView(nameField,LinearLayout.LayoutParams(0,dp(50),1f))
        nameRow.addView(button("ÁP DỤNG TÊN"){
            Config.title=nameField.text.toString().trim().ifBlank {"QUANG THIỆN"};Config.save(this);preview.invalidate()
            Toast.makeText(this,"Đã đổi thành: ${Config.title}",Toast.LENGTH_SHORT).show()
        })
        controls.addView(nameRow)
        chooser("Hiệu ứng LED (20)",Config.effects,Config.effect){Config.effect=it}
        chooser("Mẫu đồng hồ",Config.faces,Config.face){Config.face=it}
        chooser("Font chữ",Config.fonts,Config.font){Config.font=it}
        chooser("Màu RGB/chữ",Config.colors,Config.color){Config.color=it}
        controls.addView(CheckBox(this).apply{text="Giữ hai ngựa RGB tĩnh";setTextColor(Color.WHITE);isChecked=Config.horses;setOnCheckedChangeListener{_,checked->Config.horses=checked;Config.save(this@MainActivity)}})
        seek("Chiều cao LED",Config.height){Config.height=it}
        seek("Độ rộng cột",Config.barWidth){Config.barWidth=it}
        seek("Độ sáng LED",Config.brightness){Config.brightness=it}
        seek("Độ phát sáng",Config.glow){Config.glow=it}
        seek("Số cột",Config.count,130){Config.count=it.coerceAtLeast(24)}
        seek("Tốc độ",Config.speed){Config.speed=it}
        seek("BASS",Config.bass){Config.bass=it}
        seek("MID",Config.mid){Config.mid=it}
        seek("TREBLE",Config.treble){Config.treble=it}
        controls.addView(text("Bong bóng ngoài màn hình: đồng hồ kim 104dp, kéo thả, kích đúp để mở đồng hồ lớn. Bong bóng sẽ tự ẩn khi đồng hồ lớn đang mở. LED nền trong suốt, không chặn cảm ứng.",11f))
        scroll.addView(controls);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        if(Config.audioMode!=0)ensurePermissionAndStart()
        updateStatus()
    }
    private fun ensurePermissionAndStart(){
        if(Config.audioMode==0){AudioEngine.stop();updateStatus();return}
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),101);return
        }
        if(Config.audioMode==2){
            // Start microphone foreground service while activity is visible, before navigating away.
            turnOverlayOn();return
        }
        AudioEngine.start(this,Config.audioMode);updateStatus()
    }
    private fun updateStatus(){
        val mode=Config.audioModes[Config.audioMode]
        val meter=(AudioEngine.signal*100).toInt()
        status.text=if(Config.audioMode==0)"● DEMO – tín hiệu mô phỏng" else
            "● $mode | ${if(AudioEngine.live)"Đã mở nguồn" else "Chưa kết nối"} | Tín hiệu $meter% | ${AudioEngine.error}"
    }
    private fun turnOverlayOn(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            pendingOverlay=true
            Toast.makeText(this,"Cấp quyền hiển thị nổi, rồi quay lại ứng dụng",Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));return
        }
        if(Build.VERSION.SDK_INT>=23 && Config.audioMode!=0 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),101);return
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),102)
        try{
            val intent=Intent(this,OverlayService::class.java)
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent) else startService(intent)
            Toast.makeText(this,"Đang khởi động LED trong suốt + đồng hồ nổi",Toast.LENGTH_SHORT).show()
        }catch(e:Exception){Toast.makeText(this,"Không bật được: ${e.message}",Toast.LENGTH_LONG).show()}
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==101){
            if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)ensurePermissionAndStart()
            else{AudioEngine.stop();Toast.makeText(this,"Cần cấp quyền âm thanh để sử dụng Live",Toast.LENGTH_LONG).show()}
        }
    }
    override fun onResume(){super.onResume();handler.post(refresh);if(pendingOverlay && (Build.VERSION.SDK_INT<23 || Settings.canDrawOverlays(this))){pendingOverlay=false;turnOverlayOn()}}
    override fun onPause(){handler.removeCallbacks(refresh);super.onPause()}
}
