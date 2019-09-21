package com.ardog.view


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View

import com.google.ar.sceneform.samples.solarsystem.ClockActivity

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * 一个自定义钟表
 */

class ClockView : View {

    //新的圆宽

    private var sizeW: Int = 0

    private var sizeH: Int = 0

    private val timeString = arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

    private var mContext: Context? = null
    private var rectF: RectF? = null
    private var mPaint: Paint? = null
    private var mTextPaint: Paint? = null
    //缩放距离
    private val sc = 20
    //线的长度
    private var lineW = 40

    //秒表最长
    private val lineS = 360
    private var mPaintS: Paint? = null
    //分钟
    private val lineM = 260
    private var mPaintM: Paint? = null
    //时钟
    private val lineH = 220
    private var mPaintH: Paint? = null
    private var drawH: Int = 0
    private var drawM: Int = 0
    private var drawS: Int = 0
    //开始处理时间逻辑

    //开始处理角度

    //秒旋转角度
    private var angle = 6
    //分旋转角度
    private var angleM = 6
    //时旋转角度
    private var angleH = 6

    private var time = "00:00:00"
    var timeCall:(times:MutableList<String>)->Unit={}
    constructor(context: Context) : super(context) {
        initData(context)
        print("constructor 1")

    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initData(context)
        print("constructor 2")

    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initData(context)
        print("constructor 3")
    }

    private fun initData(mContext: Context) {
        this.mContext = mContext
        mPaint = Paint()
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.color = Color.parseColor("#ad0015")
        mPaint!!.isAntiAlias = true
        mPaint!!.strokeWidth = 5f
        //初始化
        mTextPaint = Paint()
        mTextPaint!!.textSize = 40f
        mTextPaint!!.isAntiAlias = true

        mPaintS = Paint()
        mPaintS!!.strokeWidth = 20f
        mPaintM = Paint()
        mPaintM!!.color = Color.parseColor("#c40302")
        mPaintM!!.strokeWidth = 5f
        mPaintM!!.isAntiAlias = true
        mPaintH = Paint()
        mPaintH!!.color = Color.parseColor("#a59632")
        mPaintH!!.strokeWidth = 5f
        mPaintH!!.isAntiAlias = true
        mPaintS!!.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        startDarwOR(canvas)
        startDrawSMH(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sizeW = w
        sizeH = h
        initW2H()
        invalidate()
        startTime()
        print("onSizeChanged")

    }


    //初始化宽高

    private fun initW2H() {

        rectF = RectF()
        rectF!!.left = sc.toFloat()
        rectF!!.right = (sizeW - sc).toFloat()
        rectF!!.top = sc.toFloat()
        rectF!!.bottom = (sizeW - sc).toFloat()


    }

    //开始画圆/表的刻度

    private fun startDarwOR(canvas: Canvas) {
        canvas.drawOval(rectF!!, mPaint!!)
        //开始画刻度表
        for (i in 0..59) {
            if (i % 5 == 0) {
                //为时间段
                lineW = 40
            } else {
                //为分钟段
                lineW = 20
            }
            if (lineW == 40) {
                //画字
                canvas.drawText(timeString[i / 5], rectF!!.bottom / 2 + sc / 2, (sc + lineW + 40).toFloat()/*加上字体大小*/, mTextPaint!!)
            }

            canvas.drawLine(rectF!!.right / 2 + sc / 2, sc.toFloat(), rectF!!.right / 2 + sc / 2, (sc + lineW).toFloat(), mPaint!!)
            canvas.rotate((360 / 60).toFloat(), rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2)

            canvas.drawOval(rectF!!.right / 2 + sc / 2 - 10, rectF!!.bottom / 2 + sc / 2 - 10, rectF!!.right / 2 + (sc / 2).toFloat() + 10f, rectF!!.bottom / 2 + (sc / 2).toFloat() + 10f, mPaintS!!)

        }
    }

    //开始画秒钟/分钟/时钟

    private fun startDrawSMH(canvas: Canvas) {
        canvas.save()
        canvas.rotate(drawH.toFloat(), rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2)
        canvas.drawLine(rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + (sc / 2).toFloat() + 50f, rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2 - lineH, mPaintH!!)
        canvas.restore()

        canvas.save()
        canvas.rotate(drawM.toFloat(), rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2)
        canvas.drawLine(rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + (sc / 2).toFloat() + 50f, rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2 - lineM, mPaintM!!)
        canvas.restore()

        canvas.save()
        canvas.rotate(drawS.toFloat(), rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2)
        canvas.drawLine(rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + (sc / 2).toFloat() + 50f, rectF!!.right / 2 + sc / 2, rectF!!.bottom / 2 + sc / 2 - lineS, mPaint!!)
        canvas.restore()

        canvas.drawText(time, rectF!!.right / 2 + sc / 2, rectF!!.bottom / 4 + sc / 2, mTextPaint!!)


    }

    private fun startTime() {

        //该逻辑处理放到子线程中
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss")

        Thread(Runnable {
            //死循环,如果有需要的同学请更改此逻辑

            while (true) {
                //获取时间

                val date = Date()
                val format = simpleDateFormat.format(date)
                val split = format.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                time = format
                if (angle > 360) {
                    angle = 6
                }
                if (angleM > 360) {
                    angleM = 6
                }
                if (angleH > 360) {
                    angleH = 6
                }

                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

               post {
                    invalidate()
                    print("hello world")
                    val h = Integer.parseInt(split[0])
                    val m = Integer.parseInt(split[1])
                    val s = Integer.parseInt(split[2])
                    drawH = angle * h
                    drawM = angleM * m
                    drawS = angleH * s
                    val msg=Message()
                    msg.obj = "$h,$m"
                    timeCall(split.toMutableList())
                    ClockActivity.handler.sendMessage(msg)

                }
            }
        }).start()


    }


}
