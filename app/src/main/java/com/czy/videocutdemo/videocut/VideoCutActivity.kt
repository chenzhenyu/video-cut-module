package com.czy.videocutdemo.videocut

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import com.czy.videocutdemo.R
import com.czy.videocutdemo.videocut.utils.ScreenUtils
import com.czy.videocutdemo.videocut.widget.VideoCutBar

/**
 * 视频裁剪（视频格式为mp4格式）
 * @author zhenyu.chen.o
 * @date 2019/3/6
 */
class VideoCutActivity : AppCompatActivity(), IVideoCutAct {
    private lateinit var ivBack: ImageView
    private lateinit var ivFirstFrame: ImageView
    private lateinit var rlVideo: RelativeLayout
    private lateinit var vVideo: VideoView
    private lateinit var ivVideoPlay: ImageView
    private lateinit var vcBar: VideoCutBar
    private lateinit var flControl: FrameLayout

    private lateinit var videoPath: String

    companion object {
        fun instance(activity: Activity, reqCode: Int, localVideoPath: String) {
            val intent = Intent(activity, VideoCutActivity::class.java)
            val bundle = Bundle()
            bundle.putString("video_path", localVideoPath)
            intent.putExtras(bundle)
            activity.startActivityForResult(intent, reqCode)
        }

        val EXTRA_PATH = "EXTRA_PATH"
        val EXTRA_START = "EXTRA_START"
        val EXTRA_DURING = "EXTRA_DURING"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        videoPath = intent.extras.getString("video_path")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_video_cut)
        initView()
    }

    private fun initView() {
        ivBack = findViewById(R.id.iv_back)
        ivFirstFrame = findViewById(R.id.iv_first_frame)
        rlVideo = findViewById(R.id.rl_video)
        vVideo = findViewById(R.id.vv_video)
        ivVideoPlay = findViewById(R.id.iv_video_play)
        vcBar = findViewById(R.id.vcb_bar)
        flControl = findViewById(R.id.fl_control)

        //关联视频播放器和控制器
        vcBar.setAssociatedVideoView(vVideo, videoPath)

        initViewWithState(ViewState.EDITABLE)
    }

    override fun changeVideoViewToEditState() {
        val lp = rlVideo.layoutParams as RelativeLayout.LayoutParams
        lp.leftMargin = ScreenUtils.dip2px(this, 48f)
        lp.rightMargin = ScreenUtils.dip2px(this, 48f)
        lp.topMargin = ScreenUtils.dip2px(this, 29f)
        lp.bottomMargin = ScreenUtils.dip2px(this, 174f)
        rlVideo.layoutParams = lp
    }

    override fun changeVideoViewToFullScreen() {
        val lp = rlVideo.layoutParams as RelativeLayout.LayoutParams
        lp.leftMargin = 0
        lp.rightMargin = 0
        lp.topMargin = 0
        lp.bottomMargin = 0
        rlVideo.layoutParams = lp
    }

    /**
     * 对应UI 待编辑，编辑中，编辑完成状态
     */
    override fun initViewWithState(state: ViewState) {
        when (state) {
            ViewState.EDITABLE -> {
                ivBack.visibility = View.VISIBLE
                ivBack.setOnClickListener {
                    finish()
                }
                //显示出视频首帧
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)
                ivFirstFrame.setImageBitmap(retriever.frameAtTime)
                ivFirstFrame.visibility = View.VISIBLE
                retriever.release()

                vVideo.stopPlayback()
                changeVideoViewToFullScreen()
                vVideo.setOnClickListener {
                    if (vVideo.isPlaying) {
                        vVideo.stopPlayback()
                        ivVideoPlay.visibility = View.VISIBLE
                        visibleEditZone()
                    }
                }
                vVideo.setOnCompletionListener {
                    ivVideoPlay.visibility = View.VISIBLE
                    visibleEditZone()
                }
                ivVideoPlay.visibility = View.VISIBLE
                ivVideoPlay.setOnClickListener {
                    loadAndShowVideo(videoPath)
                    ivFirstFrame.visibility = View.INVISIBLE
                    ivVideoPlay.visibility = View.INVISIBLE
                    hideEditZone()
                }

                vcBar.visibility = View.INVISIBLE
                vcBar.stop()

                //控制组件
                val view = View.inflate(this, R.layout.widget_video_cut_editable, null)
                view.findViewById<TextView>(R.id.tv_edit).setOnClickListener {
                    initViewWithState(ViewState.EDITING)
                }
                flControl.removeAllViews()
                flControl.addView(view)
                flControl.setBackgroundColor(resources.getColor(R.color.control_bar))
            }
            ViewState.EDITING -> {
                ivBack.visibility = View.INVISIBLE

                ivFirstFrame.visibility = View.INVISIBLE

                vVideo.stopPlayback()
                vVideo.setOnClickListener(null)
                vVideo.setOnCompletionListener(null)
                changeVideoViewToEditState()
                ivVideoPlay.visibility = View.INVISIBLE
                ivVideoPlay.setOnClickListener(null)

                vcBar.visibility = View.VISIBLE
                vcBar.start()

                //控制组件
                val view = View.inflate(this, R.layout.widget_video_cut_editing, null)
                view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
                    vcBar.resetSlide()
                    initViewWithState(ViewState.EDITABLE)
                }
                view.findViewById<TextView>(R.id.tv_next).setOnClickListener {
                    initViewWithState(ViewState.COMPLETE)
                }
                flControl.removeAllViews()
                flControl.addView(view)
                flControl.setBackgroundColor(resources.getColor(android.R.color.transparent))
            }
            ViewState.COMPLETE -> {
                ivBack.visibility = View.VISIBLE
                ivBack.setOnClickListener {
                    initViewWithState(ViewState.EDITING)
                }

                ivFirstFrame.visibility = View.INVISIBLE

                vVideo.stopPlayback()
                vVideo.setOnClickListener(null)
                vVideo.setOnCompletionListener(null)
                changeVideoViewToFullScreen()

                vcBar.visibility = View.INVISIBLE
                vcBar.start()

                ivVideoPlay.visibility = View.INVISIBLE
                ivVideoPlay.setOnClickListener(null)

                //控制组件
                val view = View.inflate(this, R.layout.widget_video_cut_complete, null)
                view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
                    vcBar.resetSlide()
                    initViewWithState(ViewState.EDITABLE)
                }
                view.findViewById<TextView>(R.id.tv_complete).setOnClickListener {
                    val clipEntity = vcBar.getClipEntity()
                    intent.putExtra(EXTRA_PATH, videoPath)
                    intent.putExtra(EXTRA_START, clipEntity.startClipPoint.toLong())
                    intent.putExtra(EXTRA_DURING, clipEntity.during.toLong())
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                flControl.removeAllViews()
                flControl.addView(view)
                flControl.setBackgroundColor(resources.getColor(android.R.color.transparent))
            }
        }
    }

    override fun loadAndShowVideo(videoPath: String) {
        //播放本地视频
        vVideo.setVideoPath(videoPath)
        vVideo.start()//开始播放
    }

    override fun visibleEditZone() {
        val anim = ObjectAnimator.ofFloat(flControl, "translationY", flControl.height.toFloat(), 0f)
        anim.duration = 500
        anim.start()
    }

    override fun hideEditZone() {
        val anim = ObjectAnimator.ofFloat(flControl, "translationY", 0f, flControl.height.toFloat())
        anim.duration = 500
        anim.start()
    }
}