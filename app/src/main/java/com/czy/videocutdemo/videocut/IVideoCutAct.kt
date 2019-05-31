package com.czy.videocutdemo.videocut

/**
 * 视频剪切页面需实现功能
 *
 * @author zhenyu.chen.o
 * @date 2019/3/6
 */
interface IVideoCutAct {
    fun changeVideoViewToEditState()
    fun changeVideoViewToFullScreen()
    fun initViewWithState(state: ViewState)
    fun loadAndShowVideo(videoPath: String)
    fun visibleEditZone()
    fun hideEditZone()
}

/**
 * 待编辑，编辑中，编辑完成
 */
enum class ViewState {
    EDITABLE, EDITING, COMPLETE
}