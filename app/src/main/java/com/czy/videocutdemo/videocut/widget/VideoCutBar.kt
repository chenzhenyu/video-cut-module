package com.czy.videocutdemo.videocut.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.VideoView
import java.util.Timer
import java.util.TimerTask
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.OnScrollListener
import com.czy.videocutdemo.R
import com.czy.videocutdemo.videocut.utils.CommUtils
import com.czy.videocutdemo.videocut.utils.ScreenUtils
import com.czy.videocutdemo.videocut.widget.model.VideoFlameModel
import com.nio.debug.sdk.ui.views.videocut.CutSlide
import com.nio.debug.sdk.ui.views.videocut.FlameAdapter
import com.nio.debug.sdk.ui.views.videocut.OnSlideListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * 视频截取选择栏（仿微信视频截取功能）
 * 功能：1、起始，终止节点可以拖拽 2、长视频帧可以左右滑动 3、可设置最大，最小区间
 * @author zhenyu.chen.o
 * @date 2019/3/4
 */
class VideoCutBar : FrameLayout {
    //默认限制视频最大时长（秒）
    private val DEFAULT_LIMIT_MAX_TIME = 15f
    //默认限制视频最小时长（秒）
    private val DEFAULT_LIMIT_MIN_TIME = 3f
    //默认限制区至屏幕边框留帧数（等同与秒）
    private val DEFAULT_LIMIT_PADDING_TIME = 3f
    //滑块的宽度
    private var DEFAULT_SLIDE_WIDTH = 0f
    //一帧图片的宽度
    private var FLAME_WIDTH = 0f
    //默认限制区宽度
    private var DEFAULT_ZONE_WIDTH = 0f

    private lateinit var contentView: View
    private lateinit var rvFlame: RecyclerView
    private lateinit var rlLimitZone: RelativeLayout
    private lateinit var vBorder: View
    private lateinit var ivIndicator: ImageView
    private lateinit var csZoneStart: CutSlide
    private lateinit var csZoneEnd: CutSlide
    private lateinit var vTopBorder: View
    private lateinit var vBottomBorder: View
    private lateinit var vShadeLeft: View
    private lateinit var vShadeRight: View
    private var vVideo: VideoView? = null
    private var videoPath: String? = null

    private lateinit var flameAdapter: FlameAdapter

    private var indicatorAnim: ValueAnimator? = null
    private var mTimer: Timer = Timer()
    private var mTimerTask: TimerTask? = null
    private var mDisposable: Disposable? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        DEFAULT_SLIDE_WIDTH = ScreenUtils.dip2px(context, 8f).toFloat()
        FLAME_WIDTH =
            (ScreenUtils.getScreenWidth(context).div(DEFAULT_LIMIT_MAX_TIME + 2f.times(DEFAULT_LIMIT_PADDING_TIME)))
        DEFAULT_ZONE_WIDTH = DEFAULT_LIMIT_MAX_TIME.times(FLAME_WIDTH)
        val slideInterval = FLAME_WIDTH * DEFAULT_LIMIT_MIN_TIME
        contentView = LayoutInflater.from(context).inflate(R.layout.widget_video_cut_bar, this, false)
        rvFlame = contentView.findViewById(R.id.rv_flame)
        rlLimitZone = contentView.findViewById(R.id.rl_limit_zone)
        vBorder = contentView.findViewById(R.id.v_border)
        ivIndicator = contentView.findViewById(R.id.iv_indicator)
        csZoneStart = contentView.findViewById(R.id.cs_zone_start)
        csZoneEnd = contentView.findViewById(R.id.cs_zone_end)
        vTopBorder = contentView.findViewById(R.id.v_top_border)
        vBottomBorder = contentView.findViewById(R.id.v_bottom_border)
        vShadeLeft = contentView.findViewById(R.id.v_shade_left)
        vShadeRight = contentView.findViewById(R.id.v_shade_right)
        //expandSlideTouchArea()
        val slideListener = object : OnSlideListener {
            /**
             * 控制两滑块间最小间隔
             */
            override fun onInterruptListener(view: CutSlide, translationX: Float): Boolean {
                return when (view.id) {
                    R.id.cs_zone_start -> {
                        (csZoneEnd.translationX - translationX) < slideInterval
                    }
                    R.id.cs_zone_end -> {
                        (translationX - csZoneStart.translationX) < slideInterval
                    }
                    else -> {
                        false
                    }
                }
            }

            /**
             * 选中区域的滑轨联动
             */
            override fun onSlide(view: CutSlide, translationX: Float) {
                vBorder.visibility = View.VISIBLE

                val topBorderlp = vTopBorder.layoutParams as RelativeLayout.LayoutParams
                val bottomBorderlp = vBottomBorder.layoutParams as RelativeLayout.LayoutParams
                val leftShadelp = vShadeLeft.layoutParams as FrameLayout.LayoutParams
                val rightShadelp = vShadeRight.layoutParams as FrameLayout.LayoutParams
                when (view.id) {
                    R.id.cs_zone_start -> {
                        //截取边界上下边线
                        //加1是为了消除Int float转换误差，都是白色重叠看不出来
                        val zoneWidth = (csZoneEnd.translationX - translationX - DEFAULT_SLIDE_WIDTH).toInt() + 1
                        val zoneTranslationX = translationX + DEFAULT_SLIDE_WIDTH
                        topBorderlp.width = zoneWidth
                        vTopBorder.layoutParams = topBorderlp
                        vTopBorder.translationX = zoneTranslationX

                        bottomBorderlp.width = zoneWidth
                        vBottomBorder.layoutParams = bottomBorderlp
                        vBottomBorder.translationX = zoneTranslationX

                        //截取边界外的阴影
                        leftShadelp.width =
                            (DEFAULT_LIMIT_PADDING_TIME.times(FLAME_WIDTH) + csZoneStart.translationX + DEFAULT_SLIDE_WIDTH).toInt()
                        vShadeLeft.layoutParams = leftShadelp

                        repeatPlayVideoBetweenZone()
                        startIndicatorAnim()
                    }
                    R.id.cs_zone_end -> {
                        //截取边界上下边线
                        val zoneWidth = (translationX - csZoneStart.translationX - DEFAULT_SLIDE_WIDTH).toInt() + 1
                        topBorderlp.width = zoneWidth
                        vTopBorder.layoutParams = topBorderlp
                        bottomBorderlp.width = zoneWidth
                        vBottomBorder.layoutParams = bottomBorderlp

                        //截取边界外的阴影
                        rightShadelp.width =
                            (DEFAULT_LIMIT_PADDING_TIME.times(FLAME_WIDTH) + DEFAULT_ZONE_WIDTH - csZoneEnd.translationX).toInt()
                        vShadeRight.layoutParams = rightShadelp

                        repeatPlayVideoBetweenZone()
                        startIndicatorAnim()
                    }
                    else -> {

                    }
                }
            }

            override fun onSlideCompleted() {
                vBorder.visibility = View.INVISIBLE
            }
        }
        csZoneStart.mOnSlide = slideListener
        csZoneEnd.mOnSlide = slideListener

        //初始化限制框距屏幕边距
        val lp = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val margin = (FLAME_WIDTH * DEFAULT_LIMIT_PADDING_TIME).toInt()
        lp.leftMargin = margin
        lp.rightMargin = margin
        rlLimitZone.layoutParams = lp

        //初始化视频帧预览 test
        flameAdapter = FlameAdapter()
        flameAdapter.setImageWidth(FLAME_WIDTH.toInt())
        rvFlame.adapter = flameAdapter
        rvFlame.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dx != 0) {//排除初始化时候的该方法的回调
                    repeatPlayVideoBetweenZone()
                    startIndicatorAnim()
                }
            }
        })

        resetSlide()

        addView(contentView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        indicatorAnim?.cancel()
        mTimerTask?.cancel()
        mTimer.cancel()
        mDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        flameAdapter.recycle()
    }

    /**
     * 滑块宽度太小赠大点击区域
     * todo 未测试功能有效
     */
    private fun expandSlideTouchArea() {
        post {
            val rectStart = Rect()
            val rectEnd = Rect()
            val expandSize = 30
            csZoneStart.getHitRect(rectStart)
            csZoneEnd.getHitRect(rectEnd)

            rectStart.top -= expandSize
            rectStart.bottom += expandSize
            rectStart.left -= expandSize
            rectStart.right += expandSize
            rectEnd.top -= expandSize
            rectEnd.bottom += expandSize
            rectEnd.left -= expandSize
            rectEnd.right += expandSize

            rlLimitZone.touchDelegate = TouchDelegate(rectStart, csZoneStart)
            rlLimitZone.touchDelegate = TouchDelegate(rectEnd, csZoneEnd)
        }
    }

    /**
     * 开始指针动画，在用户选定的区域平移一次
     */
    private fun startIndicatorAnim() {
        indicatorAnim?.cancel()
        indicatorAnim = ValueAnimator.ofFloat(csZoneStart.translationX, csZoneEnd.translationX + DEFAULT_SLIDE_WIDTH)
        indicatorAnim?.let { animator ->
            animator.duration = getClipDuring().toLong()
            animator.repeatMode = ValueAnimator.RESTART
            animator.repeatCount = ValueAnimator.INFINITE
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                ivIndicator.translationX = it.animatedValue as Float
            }
            animator.start()
        }
    }

    /**
     * 循环播放选中区域视频
     */
    private fun repeatPlayVideoBetweenZone() {
        if (flameAdapter.itemCount <= 0) return
        mTimerTask?.cancel()
        mTimerTask = object : TimerTask() {
            override fun run() {
                vVideo?.seekTo(getClipStartPoint().toInt())
                vVideo?.start()
            }
        }
        mTimer.schedule(mTimerTask, 0, getClipDuring().toLong())
    }

    /**
     * 运行在子线程
     */
    private fun extractFrameToAdapter() {
        videoPath?.let { path ->
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val videoDuration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toFloat().div(1000f)
            //视频帧
            for (i in 0 until videoDuration.toInt()) {
                //传入是微秒
                val flame = retriever.getFrameAtTime(i * 1000L * 1000L)
                val replaceIndex = i + DEFAULT_LIMIT_PADDING_TIME.toInt()
                //占位图已经放入，没有取到视频帧可以不管
                if (flame != null) {
                    //乘2是为了在横屏情况下截取的清晰
                    val scaleWidth = FLAME_WIDTH.toInt()
                    val scaleHeight = scaleWidth * 2
                    //val scaleHeight =flame.height.toFloat().div(flame.width.toFloat()).times(FLAME_WIDTH).toInt()
                    //val scaleImg = Bitmap.createScaledBitmap(flame, scaleWidth, scaleHeight, false)
                    flameAdapter.replaceData(replaceIndex, scaleFlame(flame, scaleWidth, scaleHeight))
                    flame.recycle()
                }
                post {
                    flameAdapter.notifyItemChanged(replaceIndex)
                }
            }
            retriever.release()
            return@let
        }
    }

    /**
     * 缩放关键帧缩略图(保持目标比例最大程度截取原图)
     */
    private fun scaleFlame(src: Bitmap, desWidth: Int, desHeight: Int): Bitmap {
        val srcRadio = src.width.toFloat() / src.height.toFloat()
        val desRadio = desWidth.toFloat() / desHeight.toFloat()
        return if (srcRadio > desRadio) {
            var scaleValue = desHeight.toFloat() / src.height.toFloat()
            val desScaleWidth = src.height * desRadio //缩略图按照比例拉伸以后的宽度
            val m = Matrix()
            if ((srcRadio / desRadio) > 2) {//要做清晰度补偿
                if (scaleValue < 0.5) {
                    scaleValue *= 2
                }
                m.setScale(scaleValue, scaleValue)
            } else {
                m.setScale(scaleValue, scaleValue)
            }
            Bitmap.createBitmap(
                src,
                ((src.width - desScaleWidth) / 2).toInt(),
                0,
                desScaleWidth.toInt(),
                src.height,
                m,
                false
            )
        } else {
            val scaleValue = desWidth.toFloat() / src.width.toFloat()
            val desScaleHeight = src.width / desRadio //缩略图按照比例拉伸以后的高度
            val m = Matrix()
            m.setScale(scaleValue, scaleValue)
            Bitmap.createBitmap(
                src,
                0,
                ((src.height - desScaleHeight) / 2).toInt(),
                src.width,
                desScaleHeight.toInt(),
                m,
                false
            )
        }
    }

    /**
     * 设置VideoCutBar关联的VideoView
     */
    fun setAssociatedVideoView(view: VideoView, videoPath: String) {
        this.vVideo = view
        this.videoPath = videoPath

        //视频帧预先占位
        val defaultBitmap = Bitmap.createBitmap(FLAME_WIDTH.toInt(), FLAME_WIDTH.toInt() * 3, Bitmap.Config.ARGB_8888)
        defaultBitmap.eraseColor(Color.parseColor("#000000"))
        val videoDuration = CommUtils.getVideoDuring(videoPath).toFloat().div(1000f)
        val total = videoDuration.toInt() + 2 * DEFAULT_LIMIT_PADDING_TIME.toInt()
        val defImgs = arrayListOf<VideoFlameModel>()
        for (i in 0 until total) {
            defImgs.add(VideoFlameModel())
        }
        flameAdapter.setData(defImgs)
        flameAdapter.notifyDataSetChanged()
        rlLimitZone.visibility = View.VISIBLE

        mDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        mDisposable = Observable.create<Bitmap> {
            extractFrameToAdapter()
        }.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    /**
     * 获取截取点起始时间点（millSecond）
     */
    private fun getClipStartPoint(): Float {
        //计算用户选择的截取段起始时间（结合RecyclerView和起始滑块）
        val layoutManager = rvFlame.layoutManager as LinearLayoutManager
        val position = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleChildView = layoutManager.findViewByPosition(position)
        val result = firstVisibleChildView?.let {
            val recyclerViewScrollX = position * it.width - it.left
            val startFlameDistance = recyclerViewScrollX + csZoneStart.translationX
            startFlameDistance.div(FLAME_WIDTH).times(1000f)
        }
        return result ?: 0f
    }

    /**
     * 获取截取段时长
     */
    private fun getClipDuring(): Float {
        return (DEFAULT_LIMIT_MAX_TIME.times(1000f).div(DEFAULT_ZONE_WIDTH).times(
            csZoneEnd.translationX.plus(DEFAULT_SLIDE_WIDTH).minus(csZoneStart.translationX)
        ))
    }

    /**
     * 开始动画，循环播放视频，抽取关键帧
     */
    fun start() {
        vVideo?.setVideoPath(videoPath)
        repeatPlayVideoBetweenZone()
        startIndicatorAnim()
    }

    /**
     * 结束动画，停止播放视频
     */
    fun stop() {
        mTimerTask?.cancel()
        indicatorAnim?.cancel()
    }

    /**
     * 滑块重置
     */
    fun resetSlide() {
        csZoneStart.translationX = 0f
        //初始化end滑块至右(加3是抵消误差)
        csZoneEnd.translationX = FLAME_WIDTH * DEFAULT_LIMIT_MAX_TIME - DEFAULT_SLIDE_WIDTH

        //上下边界
        val topBorderlp = vTopBorder.layoutParams as RelativeLayout.LayoutParams
        val bottomBorderlp = vBottomBorder.layoutParams as RelativeLayout.LayoutParams
        val singleBorderWidth = (csZoneEnd.translationX - csZoneStart.translationX - DEFAULT_SLIDE_WIDTH).toInt()
        topBorderlp.width = singleBorderWidth
        bottomBorderlp.width = singleBorderWidth
        vTopBorder.layoutParams = topBorderlp
        vTopBorder.translationX = DEFAULT_SLIDE_WIDTH
        vBottomBorder.layoutParams = bottomBorderlp
        vBottomBorder.translationX = DEFAULT_SLIDE_WIDTH

        //左右阴影
        val leftShadelp = vShadeLeft.layoutParams as FrameLayout.LayoutParams
        val rightShadelp = vShadeRight.layoutParams as FrameLayout.LayoutParams
        leftShadelp.width =
            (DEFAULT_LIMIT_PADDING_TIME.times(FLAME_WIDTH) + csZoneStart.translationX + DEFAULT_SLIDE_WIDTH).toInt()
        rightShadelp.width =
            (DEFAULT_LIMIT_PADDING_TIME.times(FLAME_WIDTH) + DEFAULT_ZONE_WIDTH - csZoneEnd.translationX).toInt()
        vShadeLeft.layoutParams = leftShadelp
        vShadeRight.layoutParams = rightShadelp

        rvFlame.scrollToPosition(0)
    }

    /**
     * 获取当前截取视频起始，截止时间点信息，包装在ClipEntity里
     */
    fun getClipEntity(): ClipEntity {
        val entity = ClipEntity()
        entity.startClipPoint = getClipStartPoint()
        entity.during = getClipDuring()
        return entity
    }
}

class ClipEntity {
    var startClipPoint = 0f
    var during = 0f
}