package com.google.ar.sceneform.samples.solarsystem

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
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
import com.google.ar.sceneform.ux.BaseArFragment.OnTapArPlaneListener
import kotlinx.android.synthetic.main.activity_save.UI_ArSceneView
import kotlinx.android.synthetic.main.activity_save.UI_Last
import kotlinx.android.synthetic.main.activity_save.UI_Post
import kotlinx.android.synthetic.main.activity_save.et_name
import kotlinx.android.synthetic.main.activity_save.iv_line
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager

class SaveActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )// 设置全屏
    setContentView(R.layout.activity_save)
    initView()

  }

  private fun initEdit(pos: Int) {
    et_name.visibility = View.VISIBLE
    et_name.setOnKeyListener { v, keyCode, event ->
      if (KeyEvent.KEYCODE_ENTER == keyCode && KeyEvent.ACTION_DOWN == event.action) {
        val string = et_name.text.toString()
        dataArray[pos].dataText = string
        et_name.setText("")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et_name, InputMethodManager.SHOW_FORCED)

        imm.hideSoftInputFromWindow(et_name.windowToken, 0)
        et_name.visibility = View.GONE
        return@setOnKeyListener true
      }
      false
    }
  }

  private fun initView() {
    UI_Last.setOnClickListener {
      if (isInitListener) {
        //上一步
        when (dataArray.size) {
          0 -> {
            ToastUtils.showLong("没有操作记录")
          }
          else -> {
            dataArray.remove(dataArray.last())
            (UI_ArSceneView as MyArFragment).arSceneView.scene.removeChild(
                startNodeArray
                    .removeAt(startNodeArray.size - 1)
            )
            count--
          }
        }
      } else {
        lineArray.remove(lineArray.last())
        val last = lineNodeArray.removeAt(lineNodeArray.size - 1)
        last.parent!!.removeChild(last)
      }
    }

    UI_Post.setOnClickListener {
      dataArray.forEach {
        Log.d("Crease", it.anchor.pose.toString())
      }
      generateVector2()
    }

    iv_line.setOnClickListener {
      if (isInitListener) {
        (UI_ArSceneView as MyArFragment).setOnTapArPlaneListener(null)
        iv_line.setImageResource(R.drawable.ic_clear_black_24dp)
      } else {
        (UI_ArSceneView as MyArFragment).setOnTapArPlaneListener(listener)
        iv_line.setImageResource(R.drawable.ic_call_missed_outgoing_black_24dp)
      }
      isInitListener = false
    }

    initAr()
  }

  private var isInitListener = false
  private val dataArray = arrayListOf<AnchorInfoBean>()
  private val lineArray = arrayListOf<LineStore>()
  private val sphereNodeArray = arrayListOf<Node>()
  private val lineNodeArray = arrayListOf<Node>()
  private val startNodeArray = arrayListOf<AnchorNode>()
  private val listener = OnTapArPlaneListener { hitResult, plane, motionEvent ->
    val anchorInfoBean = AnchorInfoBean("", hitResult.createAnchor(), 0.0)
    dataArray.add(anchorInfoBean)

    drawPoint(anchorInfoBean.anchor)
  }

  @SuppressLint("NewApi")
  private fun initAr() {
    isInitListener = true
    (UI_ArSceneView as MyArFragment).setOnTapArPlaneListener(listener)
  }

  private var count = 0

  private fun drawPoint(anchor: Anchor) {
    val node = AnchorNode(anchor)
    node.setParent((UI_ArSceneView as MyArFragment).arSceneView.scene)
    startNodeArray.add(node)
    val pos = startNodeArray.size - 1

    MaterialFactory.makeOpaqueWithColor(
        this@SaveActivity, com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f)
    )
        .thenAccept { material ->
          val sphere = ShapeFactory.makeSphere(0.1f, Vector3.zero(), material)
          sphereNodeArray.add(Node().apply {
            setParent(node)
            localPosition = Vector3.zero()
            renderable = sphere
            setOnTapListener { hitTestResult, motionEvent ->
              drawLine(pos)
            }
          })
        }

    ViewRenderable.builder()
        .setView(this@SaveActivity, R.layout.renderable_text)
        .build()
        .thenAccept { it ->
          (it.view as TextView).text = count.toString()
          count++
          it.isShadowCaster = false
          FaceToCameraNode().apply {
            setParent(node)
            localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
            localPosition = Vector3(0f, 0.1f, 0f)
            renderable = it
          }
        }
  }

  private var startNode: Int = -1
  private var endNode: Int = -1

  private fun drawLine(pos: Int) {
    if (isInitListener) {
      initEdit(pos)
      return
    }

    if (startNode < 0) {
      startNode = pos
      return
    }

    if (endNode >= 0) {
      return
    }

    endNode = pos

    val firstWorldPosition = startNodeArray[startNode].worldPosition
    val secondWorldPosition = startNodeArray[endNode].worldPosition

    val difference = Vector3.subtract(firstWorldPosition, secondWorldPosition)
    val directionFromTopToBottom = difference.normalized()
    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

    MaterialFactory.makeOpaqueWithColor(
        this@SaveActivity, com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f)
    )
        .thenAccept { material ->
          val lineMode = ShapeFactory.makeCube(
              Vector3(0.01f, 0.01f, difference.length()), Vector3.zero(), material
          )
          lineNodeArray.add(Node().apply {
            setParent(startNodeArray[startNode])
            renderable = lineMode
            worldPosition = Vector3.add(firstWorldPosition, secondWorldPosition)
                .scaled(0.5f)
            worldRotation = rotationFromAToB

            lineArray.add(LineStore(startNode, endNode))
          })

        }
    startNode = -1
    endNode = -1

  }

  private fun generateVector2() {
    val total = dataArray.size
    val array = Array(
        total
    ) { index ->
      Array(total) {
        if (it == index) {
          0
        } else {
          -1
        }
      }
    }

    for (lineStore in lineArray) {
      array[lineStore.startPos][lineStore.endPos] = 1
      array[lineStore.endPos][lineStore.startPos] = 1
    }

    val dogPoints = mutableListOf<DogPoint>()
    for (i in 0 until total) {
      val anchorInfoBean = dataArray[i]
      val dogPoint = DogPoint()
      dogPoint.id = i.toLong()
      dogPoint.name = anchorInfoBean.dataText
      val pose = anchorInfoBean.anchor.pose
      dogPoint.position = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
      val ids = mutableListOf<Int>()
      array[i].forEachIndexed { index, i ->
        if (i == 1) {
          ids.add(index)
        }
      }
      dogPoint.ids = ids.toTypedArray()
          .joinToString(",")
      dogPoint.rotation = floatArrayOf(pose.qx(), pose.qy(), pose.qz(), pose.qw())
      dogPoints.add(dogPoint)
    }

    PointUtil.save2File(PointUtil.list2String(dogPoints))
  }

  override fun onDestroy() {
    (UI_ArSceneView as MyArFragment).onDestroy()
    super.onDestroy()
  }
}