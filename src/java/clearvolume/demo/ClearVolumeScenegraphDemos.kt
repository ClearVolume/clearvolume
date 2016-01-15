package clearvolume.demo

import cleargl.GLMatrix
import cleargl.GLVector
import cleargl.scenegraph.Box
import cleargl.scenegraph.Camera
import cleargl.scenegraph.DetachedHeadCamera
import cleargl.scenegraph.Scene
import clearvolume.renderer.cleargl.VolumeNode
import clearvolume.renderer.factory.ClearVolumeRendererFactory
import clearvolume.renderer.opencl.OpenCLVolumeRenderer
import clearvolume.transferf.TransferFunctions
import coremem.types.NativeTypeEnum
import org.junit.Test
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * Created by ulrik on 14/12/15.
 */

class ClearVolumeScenegraphDemos {

    @Test fun demoBasicScenegraphWithVolume() {
        val lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolumeTest",
                1024,
                1024,
                NativeTypeEnum.UnsignedByte,
                1024,
                1024,
                1,
                false)

        lClearVolumeRenderer.transferFunction = TransferFunctions.getCoolWarm()
        lClearVolumeRenderer.setVisible(true)

        val myScene = Scene()
        lClearVolumeRenderer.scene = myScene
        val cam: Camera = DetachedHeadCamera()

        fun rangeRandomizer(min: Float, max: Float): Float = min + (Math.random().toFloat() * ((max - min) + 1.0f))

        var boxes = (1..20 step 1).map {
            Box(GLVector(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()))
        }

        var vnode = VolumeNode(lClearVolumeRenderer as OpenCLVolumeRenderer)
        vnode.position = GLVector(0.00f, 0.0f, 0.0f)
        vnode.model.scale(3.0f, 3.0f, 3.0f)

        boxes.map { i -> i.renderer = lClearVolumeRenderer; myScene.addChild(i); myScene.initList.add(i) }
        boxes.map { i ->
            i.position =
                    GLVector(rangeRandomizer(-10.0f, 10.0f),
                            rangeRandomizer(-10.0f, 10.0f),
                            rangeRandomizer(-10.0f, 10.0f))
        }

        val cam_view = GLMatrix()
        cam_view.setIdentity()

        val cam_proj = GLMatrix()
        cam_proj.setIdentity()
        cam_proj.setPerspectiveProjectionMatrix(
                55.0f / 180.0f * Math.PI.toFloat(),
                1.0f, 0.1f, 1000.0f)
        cam_proj.invert()

        cam.projection = cam_proj
        cam.view = cam_view
        cam.active = true
        cam.position = GLVector(0.0f, 0.0f, 0.0f)

        myScene.addChild(vnode)
        myScene.addChild(cam)
        myScene.initList.add(vnode)
        var ticks: Int = 0

        System.out.println(myScene.children)

        thread {
            Thread.sleep(1000)
            var reverse = false
            val step = 0.05f

            while (true) {
                boxes.mapIndexed {
                    i, box ->
                    box.position!!.setElement(i % 3, step * ticks)
                }

                if (ticks >= 100 && reverse == false) {
                    reverse = true
                }
                if (ticks <= 0 && reverse == true) {
                    reverse = false
                }

                if (reverse) {
                    ticks--
                } else {
                    ticks++
                }

                Thread.sleep(20)
            }
        }

        val lResolutionX = 256
        val lResolutionY = lResolutionX
        val lResolutionZ = lResolutionX

        val lVolumeDataArray = ByteArray(lResolutionX * lResolutionY * lResolutionZ)

        for (z in 0..lResolutionZ - 1)
            for (y in 0..lResolutionY - 1)
                for (x in 0..lResolutionX - 1) {
                    val lIndex = x + lResolutionX * y + lResolutionX * lResolutionY * z
                    var lCharValue = ((x.toByte().toInt() xor y.toByte().toInt() xor z.toByte().toInt()))
                    if (lCharValue < 12)
                        lCharValue = 0
                    lVolumeDataArray[lIndex] = lCharValue.toByte()
                }

        lClearVolumeRenderer.setVolumeDataBuffer(0,
                ByteBuffer.wrap(lVolumeDataArray),
                lResolutionX.toLong(),
                lResolutionY.toLong(),
                lResolutionZ.toLong())
        lClearVolumeRenderer.requestDisplay()

        while (lClearVolumeRenderer.isShowing) {
            Thread.sleep(500)
        }

        lClearVolumeRenderer.close()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun demoBasicSceneGraphWithTwoLayers() {

        val lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolumeTest",
                1024,
                1024,
                NativeTypeEnum.UnsignedByte,
                1024,
                1024,
                2,
                false)

        lClearVolumeRenderer.setVisible(true)

        val myScene = Scene()
        lClearVolumeRenderer.scene = myScene
        val cam: Camera = Camera()

        fun rangeRandomizer(min: Float, max: Float): Float = min + (Math.random().toFloat() * ((max - min) + 1.0f))

        var boxes = (1..20 step 1).map {
            Box(GLVector(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()))
        }

        var vnode1 = VolumeNode(lClearVolumeRenderer as OpenCLVolumeRenderer)
        var vnode2 = VolumeNode(lClearVolumeRenderer as OpenCLVolumeRenderer)

        vnode1.position = GLVector(1.2f, -0.7f, 0.0f)
        vnode1.model.scale(2.0f, 2.0f, 2.0f)
        vnode1.model.rotEuler(2.5, 0.4, 2.1)
        vnode1.layer = 0

        vnode2.position = GLVector(0.0f, 0.0f, 0.0f)
        vnode2.model.scale(4.0f, 4.0f, 4.0f)
        vnode2.layer = 1

        boxes.map { i -> i.renderer = lClearVolumeRenderer; myScene.addChild(i); myScene.initList.add(i) }
        boxes.map { i ->
            i.position =
                    GLVector(rangeRandomizer(-10.0f, 10.0f),
                            rangeRandomizer(-10.0f, 10.0f),
                            rangeRandomizer(-10.0f, 10.0f))
        }

        val cam_view = GLMatrix()
        cam_view.setIdentity()
        cam_view.setCamera(10.0f, 10.00f, 10.0f,
                0.0f, 0.0f, 0.1f,
                0.0f, 0.0f, 1.0f)
        cam_view.invert()

        val cam_proj = GLMatrix()
        cam_proj.setIdentity()
        cam_proj.setPerspectiveProjectionMatrix(
                55.0f / 180.0f * Math.PI.toFloat(),
                1.0f, 0.1f, 1000.0f)
        cam_proj.invert()

        cam.projection = cam_proj
        cam.view = cam_view
        cam.active = true
        cam.position = GLVector(0.0f, 0.0f, 0.0f)

        myScene.addChild(vnode1)
        myScene.addChild(vnode2)
        myScene.addChild(cam)
        myScene.initList.add(vnode1)
        myScene.initList.add(vnode2)

        var ticks: Int = 0

        System.out.println(myScene.children)

        thread {
            Thread.sleep(1000)
            var reverse = false
            val step = 0.05f

            while (true) {
                boxes.mapIndexed {
                    i, box ->
                    box.position!!.setElement(i % 3, step * ticks)
                }

                if (ticks >= 100 && reverse == false) {
                    reverse = true
                }
                if (ticks <= 0 && reverse == true) {
                    reverse = false
                }

                if (reverse) {
                    ticks--
                } else {
                    ticks++
                }

                Thread.sleep(20)
            }
        }

        val lResolutionX = 512
        val lResolutionY = lResolutionX
        val lResolutionZ = lResolutionX

        val lVolumeDataArray0 = ByteArray(lResolutionX * lResolutionY * lResolutionZ)

        for (z in 0..lResolutionZ - 1)
            for (y in 0..lResolutionY - 1)
                for (x in 0..lResolutionX - 1) {
                    val lIndex = x + lResolutionX * y + lResolutionX * lResolutionY * z
                    var lCharValue = ((x.toByte().toInt() xor y.toByte().toInt() xor z.toByte().toInt()))
                    if (lCharValue < 12)
                        lCharValue = 0
                    lVolumeDataArray0[lIndex] = lCharValue.toByte()
                }

        lClearVolumeRenderer.setVolumeDataBuffer(0,
                ByteBuffer.wrap(lVolumeDataArray0),
                lResolutionX.toLong(),
                lResolutionY.toLong(),
                lResolutionZ.toLong())


        val lVolumeDataArray1 = ByteArray(lResolutionX * lResolutionY * lResolutionZ)

        for (z in 0..lResolutionZ / 2 - 1)
            for (y in 0..lResolutionY / 2 - 1)
                for (x in 0..lResolutionX / 2 - 1) {
                    val lIndex = x + lResolutionX * y + lResolutionX * lResolutionY * z
                    var lCharValue = 255 - (((x).toByte().toInt() xor (y).toByte().toInt() xor z.toByte().toInt()))
                    if (lCharValue < 12)
                        lCharValue = 0
                    lVolumeDataArray1[lIndex] = (lCharValue).toByte()
                }

        lClearVolumeRenderer.setVolumeDataBuffer(1,
                ByteBuffer.wrap(lVolumeDataArray1),
                lResolutionX.toLong(),
                lResolutionY.toLong(),
                lResolutionZ.toLong())/**/

        lClearVolumeRenderer.requestDisplay()

        var i = 0
        while (lClearVolumeRenderer.isShowing) {
            Thread.sleep(500)
            i++
        }

        lClearVolumeRenderer.close()
    }
}
