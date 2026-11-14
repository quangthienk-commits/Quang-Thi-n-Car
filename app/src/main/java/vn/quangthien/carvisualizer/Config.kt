package vn.quangthien.carvisualizer

import android.content.Context
import android.graphics.Color

object Config {
    const val PREF = "qt_visualizer_settings"
    val effects = listOf(
        "Center Burst", "Center Ripple", "Spectrum", "Mirror Pulse", "Neon Wave",
        "Energy Tunnel", "Bass Explosion", "Laser", "Aurora", "Rainbow Chase",
        "Matrix", "Fire Dance", "Dual Peaks", "Stereo Bounce", "Neon Comet",
        "Heartbeat", "Vortex", "Stadium", "Crystal Wave", "Lightning"
    )
    val faces = listOf("Sport Blue", "Racing Red", "Luxury Gold", "Cyber Neon")
    val fonts = listOf("Thư pháp", "Chữ ký", "Brush", "Serif", "Sport Sans")
    val colors = listOf("RGB Rainbow", "Cyan", "Violet", "Gold", "Red", "Emerald", "White")
    val audioModes = listOf("Demo", "Âm thanh hệ thống", "Micro (nghe loa)")
    @Volatile var effect = 0
    @Volatile var face = 0
    @Volatile var font = 0
    @Volatile var color = 0
    @Volatile var brightness = 85
    @Volatile var glow = 75
    @Volatile var height = 75
    @Volatile var barWidth = 65
    @Volatile var count = 82
    @Volatile var speed = 70
    @Volatile var bass = 90
    @Volatile var mid = 75
    @Volatile var treble = 80
    @Volatile var horses = true
    @Volatile var title = "QUANG THIỆN"
    @Volatile var audioMode = 0 // 0 demo; 1 system Visualizer; 2 microphone
    fun load(ctx: Context) {
        val p=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        effect=p.getInt("effect",0);face=p.getInt("face",0);font=p.getInt("font",0);color=p.getInt("color",0)
        brightness=p.getInt("brightness",85);glow=p.getInt("glow",75);height=p.getInt("height",75)
        barWidth=p.getInt("barWidth",65);count=p.getInt("count",82);speed=p.getInt("speed",70)
        bass=p.getInt("bass",90);mid=p.getInt("mid",75);treble=p.getInt("treble",80)
        horses=p.getBoolean("horses",true)
        title=p.getString("title","QUANG THIỆN")?.take(32) ?: "QUANG THIỆN"
        audioMode=p.getInt("audioMode", if(p.getBoolean("demo",true)) 0 else 1).coerceIn(0,2)
    }
    fun save(ctx: Context) {
        ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().apply {
            putInt("effect",effect);putInt("face",face);putInt("font",font);putInt("color",color)
            putInt("brightness",brightness);putInt("glow",glow);putInt("height",height);putInt("barWidth",barWidth)
            putInt("count",count);putInt("speed",speed);putInt("bass",bass);putInt("mid",mid);putInt("treble",treble)
            putBoolean("horses",horses);putString("title",title);putInt("audioMode",audioMode)
        }.apply()
    }
    fun palette(index: Int, t: Float): Int {
        val hue=when(color) {
            0 -> (300f+310f*t)%360f
            1 -> 185f+30f*t
            2 -> 260f+58f*t
            3 -> 32f+18f*t
            4 -> 0f+30f*t
            5 -> 130f+35f*t
            else -> 195f
        }
        if(color==6) return Color.WHITE
        return Color.HSVToColor(floatArrayOf(hue,if(color==3) .85f else .95f,1f))
    }
}
