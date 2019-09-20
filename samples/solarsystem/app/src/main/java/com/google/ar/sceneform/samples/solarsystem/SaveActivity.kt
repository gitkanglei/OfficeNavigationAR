package com.google.ar.sceneform.samples.solarsystem

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import com.ardog.model.DogPoint
import com.ardog.utils.PointUtil
import com.blankj.utilcode.util.ToastUtils
import com.gj.arcoredraw.MyArFragment
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.activity_save.*

class SaveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)// 设置全屏
        setContentView(R.layout.activity_save)
        initView()
    }

    private fun initView() {
        UI_Last.setOnClickListener {
            //上一步
            when (dataArray.size) {
                0 -> {
                    ToastUtils.showLong("没有操作记录")
                }
                else -> {
                    dataArray.remove(dataArray.last())
                    (UI_ArSceneView as MyArFragment).arSceneView.scene.removeChild(startNodeArray
                            .removeAt(startNodeArray.size - 1))
                    count --
                }
            }
        }

        UI_Post.setOnClickListener {
            dataArray.forEach {
                Log.d("Crease", it.anchor.pose.toString())
                generateVector2(4, 4)
            }
        }
        initAr()
    }

    private val dataArray = arrayListOf<AnchorInfoBean>()
    private val sphereNodeArray = arrayListOf<Node>()
    private val startNodeArray = arrayListOf<Node>()

    @SuppressLint("NewApi")
    private fun initAr() {
        (UI_ArSceneView as MyArFragment).setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchorInfoBean = AnchorInfoBean("", hitResult.createAnchor(), 0.0)
            dataArray.add(anchorInfoBean)

            drawPoint(anchorInfoBean.anchor)
        }
    }

    private var count = 0

    private fun drawPoint(anchor: Anchor) {
        val node = AnchorNode(anchor)
        node.setParent((UI_ArSceneView as MyArFragment).arSceneView.scene)
        MaterialFactory.makeOpaqueWithColor(this@SaveActivity, com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f))
                .thenAccept { material ->
                    val sphere = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material)
                    sphereNodeArray.add(Node().apply {
                        setParent(node)
                        localPosition = Vector3.zero()
                        renderable = sphere
                    })
                }

        ViewRenderable.builder()
                .setView(this@SaveActivity, R.layout.renderable_text)
                .build()
                .thenAccept { it ->
                    (it.view as TextView).text = count.toString()
                    count ++
                    it.isShadowCaster = false
                    FaceToCameraNode().apply {
                        setParent(node)
                        localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
                        localPosition = Vector3(0f, 0.02f, 0f)
                        renderable = it
                    }
                }

        startNodeArray.add(node)
    }

    private fun generateVector2(width: Int, height: Int) {
        val total = width * height
        val array = Array<Array<Int>>(
                total
        ) {
            Array<Int>(total) {
                - 1
            }
        }
        for (i in 0 until height) {
            for (j in 0 until width) {

                val currentPos = i * width + j
                val previousPos = currentPos - 1
                val nextPos = currentPos + 1
                array[currentPos][currentPos] = 0

                if (previousPos >= 0){
                    array[currentPos][previousPos] = 1
                }

                if (nextPos < total){
                    array[currentPos][nextPos] = 1
                }

                val previousLinePos = currentPos - 2 * j - 1

                val nextLinePos = previousLinePos + width * 2

                if (previousLinePos >= 0){
                    array[currentPos][previousLinePos] = 1
                }

                if (nextLinePos < total){
                    array[currentPos][nextLinePos] = 1
                }
            }
        }

        val dogPoints = mutableListOf<DogPoint>()
        for (i in 0 until total){
            val anchorInfoBean = dataArray[i]
            val dogPoint = DogPoint()
            dogPoint.id = i.toLong()
            val pose = anchorInfoBean.anchor.pose
            dogPoint.position = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
            val ids = mutableListOf<Int>()
            array[i].forEachIndexed { index, i ->
                if (i == 1){
                    ids.add(index)
                }
            }
            dogPoint.ids = ids.toTypedArray().joinToString(",")
            dogPoint.rotation = floatArrayOf(pose.qx(), pose.qy(), pose.qz(), pose.qw())
            dogPoints.add(dogPoint)
        }

        PointUtil.save2File(PointUtil.list2String(dogPoints))
    }

    override fun onDestroy() {
        super.onDestroy()
        (UI_ArSceneView as MyArFragment).onDestroy()
    }
}