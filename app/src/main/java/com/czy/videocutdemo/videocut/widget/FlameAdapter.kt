package com.nio.debug.sdk.ui.views.videocut

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import com.czy.videocutdemo.R
import com.czy.videocutdemo.videocut.utils.ScreenUtils
import com.czy.videocutdemo.videocut.widget.model.VideoFlameModel


/**
 * 视频祯列表
 * @author zhenyu.chen.o
 * @date 2019/3/6
 */
class FlameAdapter : RecyclerView.Adapter<VH>() {
    private val imgs = mutableListOf<VideoFlameModel>()
    private var imgWidth = 0

    fun setData(datas: List<VideoFlameModel>) {
        imgs.addAll(datas)
    }

    fun replaceData(index: Int, bitmap: Bitmap): Boolean {
        if (index >= imgs.size) return false
        val replaceModel = imgs[index]
        if (replaceModel.bitmap != null) {
            replaceModel.bitmap.recycle()
        }
        replaceModel.bitmap = bitmap
        return true
    }

    fun recycle() {
        for (img in imgs) {
            if (img.bitmap != null) {
                img.bitmap.recycle()
            }
            img.bitmap = null
        }
    }

    /**
     * 添加到RecycleView之前调用有效
     */
    fun setImageWidth(width: Int) {
        imgWidth = width
    }

    override fun getItemCount(): Int {
        return imgs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        imgWidth = ScreenUtils.dip2px(parent.context, 20f)
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_video_cut_flame, parent, false)
        val lp = RelativeLayout.LayoutParams(imgWidth, LayoutParams.MATCH_PARENT)
        v.findViewById<ImageView>(R.id.iv).layoutParams = lp
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bitmap = imgs[position].bitmap
        if (bitmap == null) {//加载默认图片
            holder.iv.setImageResource(android.R.color.black)
        } else {
            holder.iv.setImageBitmap(bitmap)
        }
    }
}

class VH(v: View) : RecyclerView.ViewHolder(v) {
    val iv = v.findViewById<ImageView>(R.id.iv)
}