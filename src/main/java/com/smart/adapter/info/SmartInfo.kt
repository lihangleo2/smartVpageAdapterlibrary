package com.smart.adapter.info

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.smart.adapter.indicator.BaseIndicator
import com.smart.adapter.indicator.SmartGravity
import com.smart.adapter.indicator.SmartIndicator
import com.smart.adapter.transformer.SmartTransformer

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2024/1/5
 */
class SmartInfo {
    var fragmentManager: FragmentManager? = null
    var lifecycle: Lifecycle? = null
    val fragments = mutableMapOf<Int, Class<out Fragment>>()//fragments源

    /*
    * 兜底策略，如果没有对应type的fragment则会用defaultFragment
    * 如果没有设置defaultFragment的话，则会取fragments里第一个添加元素，如果fragments为空则会报错
    * */
    var defaultFragment: Class<out Fragment>? = null

    /*
    * 预加载litmit,当滑动到只剩余limit个数后，触发加载刷新监听
    * 如果，当前个数小于mPreLoadLimit*2+1时，优先触发loadMore监听
    * */
    var mPreLoadLimit: Int = 3
    var offscreenPageLimit: Int = -1 // viewPager2缓存
    var isVerticalFlag: Int = -1 //代码设置viewPager2横竖，-1:无设置，0:竖 1:横
    var mInfinite = false //是否开启无线循环
    var mAutoLoop = false //是否开自动滚动

    var isGallery = false //是否实现画廊

    //画廊左右margin
    var mLeftMargin: Int = 0
    var mRightMargin: Int = 0

    //是否使用指示器
    var isIndicatorFlag: Int = -1 // -1：无指示器; 0：使用者xml里使用了指示器 1：api方式指示器
    var smartIndicator: SmartIndicator = SmartIndicator.CIRCLE
    var smartGravity: SmartGravity = SmartGravity.CENTER_HORIZONTAL_BOTTOM
    var horizontalMargin: Int = 0
    var verticalMargin: Int = 0

    //
    var mBindIndicator: BaseIndicator? = null //指示器

    var isAddLifecycleObserver: Boolean = false// 是否绑定页面生命周期。一般作用域自动滚动时，页面不可见时暂停，页面回来时自动恢复滚动
    var enableScroll: Boolean = true //是否允许ViewPager2可被滑动
    var isOverScrollNever: Boolean = false //是否取消ViewPager2滑动边缘阴影
    var mLoopTime: Long = 3000L //滚动间隔时间

    var smartTransformer: SmartTransformer? = null//ViewPager2滑动效果，也支持系统方式


    var mScrollTime: Long = 600L// 轮播切换滚动速度
    var isFirstScroll = true //是否第一次设置，只需重写一次layoutManager
    var mHasSetScrollTime: Long = 0L//是否设置过滚动速度：OL表示未设置速度

    /**
     * 无数据源Adapter
     * */
    var mViewList: MutableList<View>? = null//无数据源绑定的view
    var smoothScroll: Boolean = true//点击切换tab的时候，viewPager是否需要滚动效果
}