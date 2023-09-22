package com.example.docscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import org.opencv.core.Point
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

object Utils {
    fun intersectionOverUnion(pts1: Array<Point>, pts2: Array<Point>): Float {
        val rect1 = getRectFromPoints(pts1)
        val rect2 = getRectFromPoints(pts2)
        return intersectionOverUnion(rect1, rect2)
    }

    fun intersectionOverUnion(rect1: Rect, rect2: Rect): Float {
        val leftColumnMax = Math.max(rect1.left, rect2.left)
        val rightColumnMin = Math.min(rect1.right, rect2.right)
        val upRowMax = Math.max(rect1.top, rect2.top)
        val downRowMin = Math.min(rect1.bottom, rect2.bottom)
        if (leftColumnMax >= rightColumnMin || downRowMin <= upRowMax) {
            return 0f
        }
        val s1 = rect1.width() * rect1.height()
        val s2 = rect2.width() * rect2.height()
        val sCross = ((downRowMin - upRowMax) * (rightColumnMin - leftColumnMax)).toFloat()
        return sCross / (s1 + s2 - sCross)
    }

    fun getRectFromPoints(points: Array<Point>): Rect {
        var left: Double
        var top: Double
        var right: Double
        var bottom: Double
        left = points[0].x
        top = points[0].y
        right = 0.0
        bottom = 0.0
        for (point in points) {
            left = Math.min(point.x, left)
            top = Math.min(point.y, top)
            right = Math.max(point.x, right)
            bottom = Math.max(point.y, bottom)
        }
        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    fun bitmapFromFile(file: File?): Bitmap {
        val b = readFile(file)
        return BitmapFactory.decodeByteArray(b, 0, b!!.size)
    }

    fun readFile(file: File?): ByteArray? {
        var rf: RandomAccessFile? = null
        var data: ByteArray? = null
        try {
            rf = RandomAccessFile(file, "r")
            data = ByteArray(rf.length().toInt())
            rf.readFully(data)
        } catch (exception: Exception) {
            exception.printStackTrace()
        } finally {
            closeQuietly(rf)
        }
        return data
    }

    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}