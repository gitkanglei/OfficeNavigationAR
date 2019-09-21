package com.ardog.utils

import android.content.Context
import android.os.Build
import android.widget.TextView
import com.gj.arcoredraw.MyArFragment
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.samples.solarsystem.FaceToCameraNode
import com.google.ar.sceneform.samples.solarsystem.R

/**
 * Author: kanglei
 * Version V1.0
 * Date: 2019-09-21
 * Description:
 * Modification History:
 * Date Author Version Description
 * -----------------------------------------------------------------------------------
 * 2019-09-21 kanglei 1.0
 * Why & What is modified:
 */
class DrawLineHelper {

    private val lineNodeArray = arrayListOf<Node>()
    private val sphereNodeArray = arrayListOf<Node>()
    private val startNodeArray = arrayListOf<Node>()
    private val endNodeArray = arrayListOf<Node>()

    private var arSceneView: ArSceneView? = null

    private var contxt : Context? = null

    constructor(contxt : Context,arSceneView: ArSceneView){
        this.contxt = contxt
        this.arSceneView = arSceneView
    }
    public fun drawLine(firstAnchor: Anchor, secondAnchor: Anchor) {

        val startPose = secondAnchor.pose
        val endPose = firstAnchor.pose
        val dx = startPose.tx() - endPose.tx()
        val dy = startPose.ty() - endPose.ty()
        val dz = startPose.tz() - endPose.tz()

       val length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val firstAnchorNode = AnchorNode(firstAnchor)
            startNodeArray.add(firstAnchorNode)

            val secondAnchorNode = AnchorNode(secondAnchor)
            endNodeArray.add(secondAnchorNode)

            firstAnchorNode.setParent(arSceneView?.scene)
            secondAnchorNode.setParent(arSceneView?.scene)

            MaterialFactory.makeOpaqueWithColor(contxt, com.google.ar.sceneform.rendering.Color(0.53f, 0.92f, 0f))
                    .thenAccept { material ->
                        val sphere = ShapeFactory.makeSphere(0.02f, Vector3(0.0f, 0.0f, 0.0f), material)
                        sphereNodeArray.add(Node().apply {
                            setParent(secondAnchorNode)
                            localPosition = Vector3.zero()
                            renderable = sphere
                        })
                    }

            val firstWorldPosition = firstAnchorNode.worldPosition
            val secondWorldPosition = secondAnchorNode.worldPosition

            val difference = Vector3.subtract(firstWorldPosition, secondWorldPosition)
            val directionFromTopToBottom = difference.normalized()
            val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

            MaterialFactory.makeOpaqueWithColor(contxt, com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f))
                    .thenAccept { material ->
                        val lineMode = ShapeFactory.makeCube(Vector3(0.01f, 0.01f, difference.length()), Vector3.zero(), material)
                        val lineNode = Node().apply {
                            setParent(firstAnchorNode)
                            renderable = lineMode
                            worldPosition = Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                            worldRotation = rotationFromAToB
                        }
                        lineNodeArray.add(Node().apply {
                            setParent(firstAnchorNode)
                            renderable = lineMode
                            worldPosition = Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                            worldRotation = rotationFromAToB
                        })

                        ViewRenderable.builder()
                                .setView(contxt, R.layout.renderable_text)
                                .build()
                                .thenAccept { it ->
                                    (it.view as TextView).text = "${String.format("%.1f", length * 100)}CM"
                                    it.isShadowCaster = false
                                    FaceToCameraNode().apply {
                                        setParent(lineNode)
                                        localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
                                        localPosition = Vector3(0f, 0.02f, 0f)
                                        renderable = it
                                    }
                                }
                    }
        }
    }

}