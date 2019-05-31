package com.nio.debug.sdk.ui.views.videocut

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView

/**
 * 视频剪切条上的滑块，控制起始，终止节点，包裹在RelativeLayout中
 * @author zhenyu.chen.o
 * @date 2019/3/5
 */
class CutSlide : ImageView {
    private var firstPointIndex = -1
    private var firstPointRawX: Float = -1f
    private var mTranslationX = 0f
    var mOnSlide: OnSlideListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    firstPointIndex = motionEvent.actionIndex
                    firstPointRawX = motionEvent.rawX
                    mTranslationX = translationX
                }
                MotionEvent.ACTION_MOVE -> {
                    if (motionEvent.actionIndex == firstPointIndex) {
                        val transXSrc = mTranslationX + motionEvent.rawX - firstPointRawX
                        val parentWidth = (parent as ViewGroup).width
                        val validSlideWidth = parentWidth - width
                        val translationXFinal = when {
                            transXSrc < 0 -> 0f
                            transXSrc > validSlideWidth -> validSlideWidth.toFloat()
                            else ->
                                transXSrc
                        }
                        if (mOnSlide == null || !mOnSlide!!.onInterruptListener(this, translationXFinal)) {
                            translationX = translationXFinal
                            mOnSlide?.onSlide(this, translationXFinal)
                        }
                    }
                }
                MotionEvent.ACTION_POINTER_UP,
                MotionEvent.ACTION_UP -> {
                    if (motionEvent.actionIndex == firstPointIndex) {
                        mOnSlide?.onSlideCompleted()
                        resetFirstPoint()
                    }
                }
            }
        }
        return true
    }

    private fun resetFirstPoint() {
        firstPointIndex = -1
        firstPointRawX = -1f
        mTranslationX = 0f
    }
}

/**
 * 滑块滑动的回调监听
 */
interface OnSlideListener {
    /**
     * 动画时回调
     */
    fun onSlide(view: CutSlide, translationX: Float)

    /**
     *  中断事件
     *  @return true->事件消费动画终止 false->动画继续
     */
    fun onInterruptListener(view: CutSlide, translationX: Float): Boolean

    /**
     * 滑动完成，手指抬起
     */
    fun onSlideCompleted()
}