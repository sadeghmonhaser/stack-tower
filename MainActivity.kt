package com.example.stacktower

import android.app.Activity
import android.view.Choreographer
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var game: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = GameView(this)
        setContentView(game)
    }

    override fun onPause() { super.onPause(); game.pauseLoop() }
    override fun onResume() { super.onResume(); game.resumeLoop() }
}

/** Stack Tower: tap to drop the moving block. Perfect drops grow combo & speed. */
class GameView(context: Context) : View(context) {

    private data class Block(val x: Float, val w: Float, val color: Int)

    private val blocks = mutableListOf<Block>()
    private var moving: Block? = null
    private var dir = 1
    private var x = 0f            // left edge of moving block
    private var speed = 0f
    private var y = 0f            // y of the top of the stack (set on layout)
    private var blockH = 0f
    private var score = 0
    private var combo = 0
    private var best = 0
    private var over = false
    private var hue = 0f

    // falling debris animation
    private data class Debris(var x: Float, var y: Float, var w: Float, var vy: Float, val color: Int)
    private val debris = mutableListOf<Debris>()

    // floating text ("PERFECT!")
    private var popText = ""
    private var popAlpha = 0f
    private var popY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val prefs = context.getSharedPreferences("stack", Context.MODE_PRIVATE)

    private var running = false
    private var lastNs = 0L
    private val frame = Choreographer.FrameCallback { ts ->
        val dt = if (lastNs == 0L) 1f else ((ts - lastNs) / 16_666_667f).coerceAtMost(3f)
        lastNs = ts
        update(dt)
        if (running) Choreographer.getInstance().postFrameCallback(frame)
    }

    init {
        best = prefs.getInt("best", 0)
        setBackgroundColor(Color.rgb(20, 24, 45))
    }

    fun pauseLoop() { running = false; lastNs = 0L }
    fun resumeLoop() {
        if (!running) { running = true; lastNs = 0L; Choreographer.getInstance().postFrameCallback(frame) }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        blockH = w * 0.09f
        y = h * 0.82f
        resetGame()
        resumeLoop()
    }

    private fun resetGame() {
        blocks.clear(); debris.clear()
        val baseW = width * 0.55f
        blocks.add(Block((width - baseW) / 2f, baseW, colorFor(0)))
        score = 0; combo = 0; over = false; hue = 0f
        speed = width * 0.006f
        spawn()
    }

    private fun colorFor(i: Int): Int =
        Color.HSVToColor(floatArrayOf((i * 17f) % 360f, 0.55f, 0.95f))

    private fun spawn() {
        val top = blocks.last()
        moving = Block(0f, top.w, colorFor(blocks.size))
        dir = if (blocks.size % 2 == 0) 1 else -1
        x = if (dir > 0) 0f else width - top.w
    }

    /** Frame update. Called ~60 times per second. */
    private fun update(dt: Float) {
        if (width == 0) return
        if (!over) {
            val m = moving ?: return
            x += speed * dir * dt
            val limit = width - m.w
            if (x <= 0f) { x = 0f; dir = 1 }
            if (x >= limit) { x = limit; dir = -1 }
        }
        // debris
        val it = debris.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.vy += 0.9f * dt
            d.y += d.vy * dt
            if (d.y > height) it.remove()
        }
        if (popAlpha > 0f) { popAlpha -= 0.02f * dt; popY -= 1.2f * dt }
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            if (over) resetGame() else drop()
        }
        return true
    }

    private fun drop() {
        val m = moving ?: return
        val top = blocks.last()
        val left = max(m.x, top.x)
        val right = min(m.x + m.w, top.x + top.w)
        val overlap = right - left
        if (overlap <= 0f) {
            // missed completely
            debris.add(Debris(m.x, y - blocks.size * blockH + 0f, m.w, 0f, m.color))
            endGame()
            return
        }
        val perfect = abs(m.x - top.x) < width * 0.012f && abs(m.w - top.w) < 2f
        val newBlock: Block
        if (perfect) {
            combo++
            newBlock = Block(top.x, top.w, m.color)
            popText = if (combo >= 3) "PERFECT x$combo!" else "PERFECT!"
        } else {
            combo = 0
            newBlock = Block(left, overlap, m.color)
            // chopped piece falls away
            val cutLeft = if (m.x < top.x) m.x else right
            val cutW = m.w - overlap
            if (cutW > 0f) debris.add(Debris(cutLeft, y - blocks.size * blockH, cutW, 0f, m.color))
        }
        blocks.add(newBlock)
        score++
        popAlpha = if (perfect) 1f else 0f
        popY = y - blocks.size * blockH - blockH
        if (!perfect) popText = ""
        if (score % 5 == 0) {
            hue = (hue + 25f) % 360f
            speed *= 1.08f
        } else {
            speed *= 1.02f
        }
        // camera scroll is simply recomputed in onDraw; keep stack tidy
        if (blocks.size > 1000) blocks.removeAt(0)
        spawn()
        invalidate()
    }

    private fun endGame() {
        over = true
        moving = null
        if (score > best) {
            best = score
            prefs.edit().putInt("best", best).apply()
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()

        // background gradient
        paint.shader = LinearGradient(0f, 0f, 0f, h,
            Color.rgb(30, 36, 70), Color.rgb(10, 12, 25), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // stack scrolls down when it grows high
        val maxVisible = ((y) / blockH).toInt()
        val shift = if (blocks.size > maxVisible - 3) (blocks.size - (maxVisible - 3)) * blockH else 0f

        // placed blocks
        for ((i, b) in blocks.withIndex()) {
            val by = y - i * blockH + shift
            paint.color = b.color
            c.drawRoundRect(b.x, by - blockH + 4f, b.x + b.w, by, 10f, 10f, paint)
        }

        // debris
        for (d in debris) {
            paint.color = d.color
            c.drawRect(d.x, d.y + shift, d.x + d.w, d.y + blockH + shift, paint)
        }

        // moving block
        moving?.let { m ->
            val my = y - blocks.size * blockH + shift
            paint.color = m.color
            c.drawRoundRect(x, my - blockH + 4f, x + m.w, my, 10f, 10f, paint)
        }

        // ground line
        paint.color = Color.argb(60, 255, 255, 255)
        c.drawRect(0f, y + shift, w, y + shift + 3f, paint)

        // HUD
        textPaint.textSize = w * 0.18f
        c.drawText(score.toString(), w / 2f, h * 0.14f, textPaint)
        textPaint.textSize = w * 0.04f
        c.drawText("Best: $best", w / 2f, h * 0.14f + w * 0.07f, textPaint)

        if (popAlpha > 0f && popText.isNotEmpty()) {
            textPaint.textSize = w * 0.07f
            textPaint.alpha = (popAlpha * 255).toInt().coerceIn(0, 255)
            c.drawText(popText, w / 2f, popY, textPaint)
            textPaint.alpha = 255
        }

        if (!over && blocks.size == 1 && score == 0) {
            textPaint.textSize = w * 0.06f
            c.drawText("ضربه بزن تا شروع شود", w / 2f, h * 0.6f, textPaint)
            c.drawText("Tap to drop", w / 2f, h * 0.6f + w * 0.08f, textPaint)
        }

        if (over) {
            paint.color = Color.argb(170, 0, 0, 0)
            c.drawRect(0f, 0f, w, h, paint)
            textPaint.textSize = w * 0.1f
            c.drawText("GAME OVER", w / 2f, h * 0.42f, textPaint)
            textPaint.textSize = w * 0.06f
            c.drawText("Score: $score", w / 2f, h * 0.50f, textPaint)
            c.drawText("Tap to retry", w / 2f, h * 0.58f, textPaint)
        }
    }
}
