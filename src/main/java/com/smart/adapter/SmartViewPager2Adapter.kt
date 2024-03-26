package com.smart.adapter

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.smart.adapter.indicator.BaseIndicator
import com.smart.adapter.indicator.CircleIndicator
import com.smart.adapter.indicator.LineIndicator
import com.smart.adapter.indicator.SmartGravity
import com.smart.adapter.indicator.SmartIndicator
import com.smart.adapter.info.SmartInfo
import com.smart.adapter.interf.OnLoadMoreListener
import com.smart.adapter.interf.OnRefreshListener
import com.smart.adapter.interf.OnRefreshLoadMoreListener
import com.smart.adapter.interf.SmartFragmentImpl
import com.smart.adapter.interf.SmartFragmentTypeExEntity
import com.smart.adapter.interf.onSideListener
import com.smart.adapter.layoutmanager.ScrollSpeedManger
import com.smart.adapter.transformer.SmartTransformer
import com.smart.adapter.transformer.StereoPagerTransformer
import com.smart.adapter.transformer.StereoPagerVerticalTransformer
import com.smart.adapter.transformer.TransAlphScaleFormer
import com.smart.adapter.util.ScreenUtils
import com.smart.adapter.util.ViewPager2Util
import java.lang.ref.WeakReference


/**
 * @Author leo2
 * @Date 2023/9/4
 */
class SmartViewPager2Adapter<T : SmartFragmentTypeExEntity> : FragmentStateAdapter {
    private lateinit var smartInfo: SmartInfo
    private lateinit var mViewPager2: ViewPager2

    private val mDataList = mutableListOf<T>()
    private val mPreloadDataList = mutableListOf<T>()

    private var mRefreshListener: OnRefreshListener? = null
    private var mLoadMoreListener: OnLoadMoreListener? = null
    private var mSideListener: onSideListener? = null

    private val mLoopTask by lazy {
        AutoLoopTask(mViewPager2, this)
    }

    private val mLifecycleEventObserver by lazy {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    start()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    stop()
                }

                Lifecycle.Event.ON_DESTROY -> {

                }

                else -> {}
            }
        }
    }


    //左右边缘滑动监听
    private var mStartSideXorY = 0

    //临界值触发
    private val criticalValue = 200

    //滑动方向
    private var mSwapFlag = 0

    private constructor(smartInfo: SmartInfo, viewPager2: ViewPager2) : super(smartInfo.fragmentManager!!, smartInfo.lifecycle!!) {
        this.smartInfo = smartInfo
        this.mViewPager2 = viewPager2
        initSmartViewPager()
    }

    /**
     * 1.数据类api
     * *********************************************************************************************************
     */
    fun addData(@NonNull list: Collection<T>): SmartViewPager2Adapter<T> {
        if (list.isNullOrEmpty()) {
            return this
        }
        var lastIndex = mDataList.size
        mDataList.addAll(list)
        if (this.smartInfo.mInfinite) {
            updateDataWithInfinite()
        } else {
            updateLoadmore(lastIndex, list.size)
        }
        updateRefreshLoadMoreState()
        notifyDataSetIndicator(this.smartInfo.mBindIndicator)
        return this
    }

    fun addData(bean: T): SmartViewPager2Adapter<T> {
        if (bean == null) {
            return this
        }
        addData(mutableListOf(bean))
        return this
    }

    fun addFrontData(@NonNull list: Collection<T>): SmartViewPager2Adapter<T> {
        if (list.isNullOrEmpty()) {
            return this
        }
        mPreloadDataList.addAll(0, list)
        updateWithIdel(mViewPager2.scrollState)
        return this
    }

    fun addFrontData(bean: T): SmartViewPager2Adapter<T> {
        if (bean == null) {
            return this
        }
        addFrontData(mutableListOf(bean))
        return this
    }

    //移除数据
    fun removeData(index: Int): SmartViewPager2Adapter<T> {
        notifyItemRemoved(index)
        mDataList.removeAt(index)
        return this
    }


    fun getItem(@IntRange(from = 0) position: Int): T {
        return if (this.smartInfo.mInfinite) {
            mDataList[position % mDataList.size]
        } else {
            mDataList[position]
        }
    }

    fun getLastItem(): T? {
        return mDataList.lastOrNull()
    }

    fun getItemOrNull(@IntRange(from = 0) position: Int): T? {
        return if (this.smartInfo.mInfinite) {
            mDataList.getOrNull(position % mDataList.size)
        } else {
            mDataList.getOrNull(position)
        }
    }

    //只允许获取数据
    fun getData(): List<T> {
        return mDataList
    }


    //如果返回 -1，表示不存在
    fun getItemPosition(item: T?): Int {
        return if (item != null && mDataList.isNotEmpty()) mDataList.indexOf(item) else -1
    }

    /***
     * *********************************************************************************************************
     */


    /**
     * 2.监听类api
     * *********************************************************************************************************
     */
    fun setOnRefreshListener(listener: OnRefreshListener): SmartViewPager2Adapter<T> {
        this.mRefreshListener = listener
        return this
    }

    /*
    * 注意：当列表无数据时（或小于mPreLoadLimit）会触发加载监听。也就是只需要在加载监听里加上网络请求即可
    * */
    fun setOnLoadMoreListener(listener: OnLoadMoreListener): SmartViewPager2Adapter<T> {
        this.mLoadMoreListener = listener
        checkIndexAndCallBack(mViewPager2.currentItem)
        return this
    }

    fun setOnRefreshLoadMoreListener(listener: OnRefreshLoadMoreListener): SmartViewPager2Adapter<T> {
        this.mRefreshListener = listener
        this.mLoadMoreListener = listener
        checkIndexAndCallBack(mViewPager2.currentItem)
        return this
    }

    fun setOnSideListener(listener: onSideListener): SmartViewPager2Adapter<T> {
        this.mSideListener = listener
        return this
    }

    /***
     * *********************************************************************************************************
     */


    /**
     * 3.方法
     * *********************************************************************************************************
     */

    /*
    * viewPager2是否可手势滑动
    * */
    fun canScroll(enableScroll: Boolean): SmartViewPager2Adapter<T> {
        mViewPager2.isUserInputEnabled = enableScroll
        return this
    }


    /*
    * 取消viewPager2边缘滑动阴影
    * */
    fun overScrollNever(): SmartViewPager2Adapter<T> {
        ViewPager2Util.cancleViewPagerShadow(mViewPager2)
        return this
    }

    /*
    * 设置viewPager2滚动间隔时间（自动滚动）
    * */
    fun setLoopTime(loopTime: Long): SmartViewPager2Adapter<T> {
        this.smartInfo.mLoopTime = loopTime
        //限定最小滚动时间
        if (this.smartInfo.mLoopTime < 500L) {
            this.smartInfo.mLoopTime = 500L
        }
        return this
    }

    /*
    * 设置viewPager2滚动切换速度
    * */
    fun setScrollTime(scrollTime: Long): SmartViewPager2Adapter<T> {
        this.smartInfo.mScrollTime = scrollTime
        if (this.smartInfo.isFirstScroll) {
            this.smartInfo.isFirstScroll = false
            ScrollSpeedManger.reflectLayoutManager(mViewPager2, this)
        }
        return this
    }

    fun getScrollTime(): Long {
        return this.smartInfo.mScrollTime
    }


    /*
    * 设置viewPager2滑动效果
    * */
    fun setPagerTransformer(smartTransformer: SmartTransformer): SmartViewPager2Adapter<T> {
        when (smartTransformer) {
            SmartTransformer.TRANSFORMER_3D -> {
                mViewPager2.setPageTransformer(
                    if (mViewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                        StereoPagerTransformer(mViewPager2.resources.displayMetrics.widthPixels.toFloat())
                    } else {
                        StereoPagerVerticalTransformer(mViewPager2.resources.displayMetrics.heightPixels.toFloat())
                    }
                )
            }

            SmartTransformer.TRANSFORMER_ALPHA_SCALE -> mViewPager2.setPageTransformer(
                TransAlphScaleFormer()
            )
        }
        return this
    }

    /*
    * 是否绑定页面生命周期
    * 一般作用域自动滚动时，页面不可见时暂停，页面回来时自动恢复滚动
    * */
    fun addLifecycleObserver(): SmartViewPager2Adapter<T> {
        removeLifecycleObserver()
        this.smartInfo.lifecycle?.addObserver(mLifecycleEventObserver)
        return this
    }

    fun removeLifecycleObserver() {
        this.smartInfo.lifecycle?.removeObserver(mLifecycleEventObserver)
    }

    /**
     * 调用此方法将结束refresh，滑动后会继续触发，
     * （此次接口请求异常等情况，又没有做重试机制，可调用此方法结束此次加载）
     * */
    fun finishRefresh() {
        updateRefreshLoadMoreState()
    }


    /**
     * 调用此方法将结束LoadMore，同上
     * */
    fun finishLoadMore() {
        updateRefreshLoadMoreState()
    }


    /**
     * 调用此方法将不再触发refresh监听
     * */
    fun finishRefreshWithNoMoreData() {
        hasRefresh = false
        updateRefreshLoadMoreState()
    }


    /**
     * 调用此方法将不再触发LoadMore监听
     * */
    fun finishLoadMoreWithNoMoreData() {
        hasLoadMore = false
        updateRefreshLoadMoreState()
    }


    fun start(): SmartViewPager2Adapter<T> {
        stop()
        mViewPager2.postDelayed(mLoopTask, this.smartInfo.mLoopTime)
        return this
    }

    fun stop(): SmartViewPager2Adapter<T> {
        mViewPager2.removeCallbacks(mLoopTask)
        return this
    }


    /***
     * *********************************************************************************************************
     */

    override fun createFragment(position: Int): Fragment {

        var bean = if (this.smartInfo.mInfinite) {
            mDataList[position % mDataList.size]
        } else {
            mDataList[position]
        }
        if (this.smartInfo.fragments.isEmpty()) {
            throw IllegalArgumentException("the fragments can not be empty,add your fragments")
        }
        var targetFragment = this.smartInfo.fragments[bean.getFragmentType()]
        if (targetFragment == null) {
            targetFragment = this.smartInfo.defaultFragment
        }
        var realFragment = targetFragment?.newInstance()

        if (realFragment !is SmartFragmentImpl<*>) {
            throw IllegalArgumentException("your fragment must implements SmartFragmentImpl<T>")
        }
        var smartFrgamentImpl = realFragment as SmartFragmentImpl<T>
        smartFrgamentImpl.initSmartFragmentData(bean)
        Log.e("createFragment", "+++++++++++++++++++++++++++++  createFragment$position")
        return realFragment
    }

    override fun getItemCount() = if (this.smartInfo.mInfinite) {
        Int.MAX_VALUE / mDataList.size * mDataList.size
    } else {
        mDataList.size
    }

    override fun getItemId(position: Int): Long {
        var beanExEntity = mDataList[position % mDataList.size]
        return if (this.smartInfo.mInfinite) {
            (mDataList[position % mDataList.size].hashCode() + position).toLong()
        } else {
            if (beanExEntity.smartViewPagerId == 0L) {
                beanExEntity.smartViewPagerId = mDataList[position % mDataList.size].hashCode().toLong()
            }
            beanExEntity.smartViewPagerId
        }
    }


    private fun initSmartViewPager() {

        if (mViewPager2 == null) {
            throw IllegalArgumentException("the bindView viewPager2 can not be null")
        }

        canScroll(this.smartInfo.enableScroll)

        if (this.smartInfo.isOverScrollNever) {
            ViewPager2Util.cancleViewPagerShadow(mViewPager2)
        }

        if (this.smartInfo.isAddLifecycleObserver) {
            addLifecycleObserver()
        }
        if (this.smartInfo.smartTransformer != null) {
            setPagerTransformer(this.smartInfo.smartTransformer!!)
        }

        if (this.smartInfo.mHasSetScrollTime != 0L) {
            setScrollTime(this.smartInfo.mHasSetScrollTime)
        }

        setLoopTime(this.smartInfo.mLoopTime)



        mViewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                smartInfo.mBindIndicator?.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                updateWithIdel(state)
                smartInfo.mBindIndicator?.onPageScrollStateChanged(state)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                checkIndexAndCallBack(position)
                smartInfo.mBindIndicator?.onPageSelected(position)
            }
        })


        registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (smartInfo.mInfinite) {
                    var wholeNum = itemCount / 2 / mDataList.size
                    mViewPager2.post {
                        //-mDataList.size 解决一致添加数据时，滚动问题
                        mViewPager2.setCurrentItem(if (mViewPager2.currentItem != wholeNum * mDataList.size) wholeNum * mDataList.size else wholeNum * mDataList.size - mDataList.size, false)
                    }
                }
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)

            }

        })

        cancleSaveEnabled()
        setTouchListenerForViewPager2()
    }

    private fun checkIndexAndCallBack(position: Int) {
        //如果是无线循环模式，则不支持监听
        if (this.smartInfo.mInfinite) {
            selectPosWithInfinite(position)
            return
        }

        if (mLoadMoreListener != null) {
            registLoadMoreOrNot(position)
        }
        if (mRefreshListener != null) {
            registRefreshOrNot(position)
        }
    }

    private fun setTouchListenerForViewPager2() {
        mViewPager2.getChildAt(0).setOnTouchListener { _, p1 ->
            when (p1?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mLoopTask != null) {
                        stop()
                    }

                    //边缘滑动监听，区分横竖方向
                    mStartSideXorY = if (mViewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                        p1.x.toInt()
                    } else {
                        p1.y.toInt()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    //区分横竖方向
                    if (mViewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                        if (mStartSideXorY - p1.x > criticalValue && mViewPager2.currentItem == itemCount - 1) {
                            mSwapFlag = 1
                        }
                        if (p1.x - mStartSideXorY > criticalValue && mViewPager2.currentItem == 0) {
                            mSwapFlag = -1
                        }
                    } else {
                        if (mStartSideXorY - p1.y > criticalValue && mViewPager2.currentItem == itemCount - 1) {
                            mSwapFlag = 1
                        }
                        if (p1.y - mStartSideXorY > criticalValue && mViewPager2.currentItem == 0) {
                            mSwapFlag = -1
                        }
                    }

                }

                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP,
                -> {
                    if (this.smartInfo.mAutoLoop) {
                        start()
                    }

                    if (mSwapFlag == 1) {
                        mSideListener?.onRightSide()
                    } else if (mSwapFlag == -1) {
                        mSideListener?.onLeftSide()
                    }
                    mSwapFlag = 0
                }
            }
            false
        }
    }

    private fun cancleSaveEnabled() {
        mViewPager2.isSaveEnabled = false
    }

    private fun updateLoadmore(tempSize: Int, netDataSize: Int) {
        notifyItemRangeChanged(tempSize, netDataSize)
    }

    private fun updateRefresh(tempSize: Int) {
        if (tempSize == 0) {
            //初始化数据为空时，front只能走notifyDataSetChanged
            notifyDataSetChanged()
        } else {
            mViewPager2.setCurrentItem(mPreloadDataList.size + mViewPager2.currentItem, false)
        }
    }

    //无线循环下，更新
    private fun updateDataWithInfinite() {
        notifyDataSetChanged()
    }

    //无线循环下，选择正确pos
    private fun selectPosWithInfinite(position: Int) {
        if (position <= Int.MAX_VALUE / 4 && mViewPager2.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
            var wholeNum = Int.MAX_VALUE / 2 / mDataList.size
            mViewPager2.post {
                mViewPager2.setCurrentItem(wholeNum * mDataList.size + position, false)
            }
        }
    }


    /*
    * 空闲时才去更新数据
    * */
    private fun updateWithIdel(state: Int) {
        if (state == ViewPager2.SCROLL_STATE_IDLE && mPreloadDataList.isNotEmpty()) {
            var tempSize = mDataList.size
            mDataList.addAll(0, mPreloadDataList)
            if (this.smartInfo.mInfinite) {
                updateDataWithInfinite()
            } else {
                updateRefresh(tempSize)
            }
            mPreloadDataList.clear()
            updateRefreshLoadMoreState()
            notifyDataSetIndicator(this.smartInfo.mBindIndicator)
        }

        if (this.smartInfo.mInfinite) {
            selectPosWithInfinite(mViewPager2.currentItem)
        }
    }


    private var hasRefresh = true
    private var isRefreshing = false
    private fun registRefreshOrNot(currentPosition: Int) {
        //刷新和加载，同一时间段只允许一个进行(直至数据返回，或主动调用finishWithNoData)
        if (!hasRefresh || isRefreshing || isLoadMoring) {
            return
        }

        if (currentPosition <= this.smartInfo.mPreLoadLimit) {
            isRefreshing = true
            mRefreshListener?.onRefresh(this)
        }
    }

    private fun updateRefreshLoadMoreState() {
        isRefreshing = false
        isLoadMoring = false
    }


    private var hasLoadMore = true
    private var isLoadMoring = false
    private fun registLoadMoreOrNot(currentPosition: Int) {
        if (!hasLoadMore || isLoadMoring || isRefreshing) {
            return
        }

        val realPosition: Int = mDataList.size - 1 - currentPosition
        if (realPosition <= this.smartInfo.mPreLoadLimit) {
            isLoadMoring = true
            mLoadMoreListener?.onLoadMore(this)
        }
    }

    internal class AutoLoopTask(viewPager2: ViewPager2, smartViewPager2Adapter: SmartViewPager2Adapter<*>) : Runnable {
        private val referencePager: WeakReference<ViewPager2>
        private val referenceAdapter: WeakReference<SmartViewPager2Adapter<*>>

        init {
            referencePager = WeakReference<ViewPager2>(viewPager2)
            referenceAdapter = WeakReference<SmartViewPager2Adapter<*>>(smartViewPager2Adapter)
        }

        override fun run() {
            val viewPager2: ViewPager2? = referencePager.get()
            val smartViewPager2Adapter: SmartViewPager2Adapter<*>? = referenceAdapter.get()
            if (viewPager2 != null) {
                val count: Int = smartViewPager2Adapter!!.itemCount
                if (count == 0) {
                    return
                }
                //没有设置无线循，且size=1时，不进行滚动
                if (!smartViewPager2Adapter.smartInfo.mInfinite && count == 1) {
                    return
                }

                //没有设置无线循环时，达到最大size之后重置
                if (!smartViewPager2Adapter.smartInfo.mInfinite) {
                    if (viewPager2.currentItem == smartViewPager2Adapter.mDataList.size - 1) {
                        viewPager2.currentItem = 0
                    } else {
                        viewPager2.currentItem = viewPager2.currentItem + 1
                    }
                } else {
                    viewPager2.currentItem = viewPager2.currentItem + 1
                }
                viewPager2.postDelayed(smartViewPager2Adapter.mLoopTask, smartViewPager2Adapter.smartInfo.mLoopTime)
            }
        }
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (this.smartInfo.mAutoLoop) {
            start()
        }
    }


    private fun notifyDataSetIndicator(indicator: BaseIndicator?) {
        indicator?.let {
            indicator?.setTotalCount(mDataList.size)
            indicator?.setCurrentIndex(mViewPager2.currentItem)
            indicator?.notifyDataSetChanged()
        }
    }


    class Builder<T : SmartFragmentTypeExEntity> {
        private val smartInfo: SmartInfo = SmartInfo()
        private var context: Context? = null

        constructor(fragmentActivity: FragmentActivity) {
            this.smartInfo.fragmentManager = fragmentActivity.supportFragmentManager
            this.smartInfo.lifecycle = fragmentActivity.lifecycle
            this.context = fragmentActivity
        }

        constructor(fragment: Fragment) {
            this.smartInfo.fragmentManager = fragment.childFragmentManager
            this.smartInfo.lifecycle = fragment.lifecycle
            this.context = fragment.requireContext()
        }

        fun addDefaultFragment(fragment: Class<out Fragment>): Builder<T> {
            this.smartInfo.defaultFragment = fragment
            return this
        }

        fun addFragment(type: Int, fragment: Class<out Fragment>): Builder<T> {
            this.smartInfo.fragments[type] = fragment
            if (this.smartInfo.defaultFragment == null) {
                this.smartInfo.defaultFragment = this.smartInfo.fragments[type]
            }
            return this
        }

        fun setPreLoadLimit(preLoadLimit: Int): Builder<T> {
            this.smartInfo.mPreLoadLimit = preLoadLimit
            return this
        }

        fun setOffscreenPageLimit(limit: Int): Builder<T> {
            this.smartInfo.offscreenPageLimit = limit
            return this
        }


        fun setVertical(isVertical: Boolean): Builder<T> {
            this.smartInfo.isVerticalFlag = if (isVertical) {
                0
            } else {
                1
            }
            return this
        }

        fun setInfinite(isInfinite: Boolean = true): Builder<T> {
            this.smartInfo.mInfinite = isInfinite
            return this
        }


        fun setAutoLoop(autoLoop: Boolean = true): Builder<T> {
            this.smartInfo.mAutoLoop = autoLoop
            return this
        }


        fun asGallery(leftMargin: Int, rightMargin: Int): Builder<T> {
            this.smartInfo.isGallery = true
            this.smartInfo.mLeftMargin = leftMargin
            this.smartInfo.mRightMargin = rightMargin
            return this
        }

        fun withIndicator(smartIndicator: SmartIndicator = SmartIndicator.CIRCLE, smartGravity: SmartGravity = SmartGravity.CENTER_HORIZONTAL_BOTTOM, horizontalMargin: Int = context!!.resources.getDimension(R.dimen.default_bottom_margin).toInt(), verticalMargin: Int = context!!.resources.getDimension(R.dimen.default_bottom_margin).toInt()): Builder<T> {
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.isIndicatorFlag = 1
                this.smartInfo.smartIndicator = smartIndicator
                this.smartInfo.smartGravity = smartGravity
                this.smartInfo.horizontalMargin = horizontalMargin
                this.smartInfo.verticalMargin = verticalMargin
            }
            return this
        }

        fun withIndicator(mBindIndicator: BaseIndicator): Builder<T> {
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.isIndicatorFlag = 0
                this.smartInfo.mBindIndicator = mBindIndicator
            }
            return this
        }

        fun addLifecycleObserver(): Builder<T> {
            this.smartInfo.isAddLifecycleObserver = true
            return this
        }

        fun canScroll(enableScroll: Boolean): Builder<T> {
            this.smartInfo.enableScroll = enableScroll
            return this
        }

        fun overScrollNever(): Builder<T> {
            this.smartInfo.isOverScrollNever = true
            return this
        }

        fun setLoopTime(loopTime: Long): Builder<T> {
            this.smartInfo.mLoopTime = loopTime
            return this
        }

        fun setPagerTransformer(smartTransformer: SmartTransformer): Builder<T> {
            this.smartInfo.smartTransformer = smartTransformer
            return this
        }


        fun setScrollTime(scrollTime: Long): Builder<T> {
            this.smartInfo.mHasSetScrollTime = scrollTime
            return this
        }


        public fun build(viewPager2: ViewPager2): SmartViewPager2Adapter<T> {
            if (this.smartInfo.offscreenPageLimit != -1) {
                viewPager2.offscreenPageLimit = this.smartInfo.offscreenPageLimit
            }

            if (this.smartInfo.isVerticalFlag != -1) {
                viewPager2.orientation = if (this.smartInfo.isVerticalFlag == 0) {
                    ViewPager2.ORIENTATION_VERTICAL
                } else {
                    ViewPager2.ORIENTATION_HORIZONTAL
                }
            }

            if (this.smartInfo.isGallery) {
                var recycleView = ViewPager2Util.getRecycleFromViewPager2(viewPager2)
                if (viewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                    recycleView?.setPadding(this.smartInfo.mLeftMargin, 0, this.smartInfo.mRightMargin, 0)
                } else {
                    recycleView?.setPadding(0, this.smartInfo.mLeftMargin, 0, this.smartInfo.mRightMargin)
                }
                recycleView?.clipToPadding = false
            }

            if (this.smartInfo.isIndicatorFlag == 1) {
                createCircleIndicator(this.smartInfo.smartIndicator, this.smartInfo.smartGravity, this.smartInfo.horizontalMargin, this.smartInfo.verticalMargin, viewPager2)
            }

            return SmartViewPager2Adapter(this.smartInfo, viewPager2)
        }

        private fun createCircleIndicator(smartIndicator: SmartIndicator, smartGravity: SmartGravity, horizontalMargin: Int, verticalMargin: Int, mViewPager2: ViewPager2) {
            //因为不是自定义view,利用viewPager2的父布局去添加指示器，此api需要viewPager2的父布局为ConstraintLayout
            if (mViewPager2.parent !is ConstraintLayout) {
                //使用者viewPager2的父布局不是ConstraintLayout，可以在xml里使用具体demo有演示（这种方式，可以使用指示器所有自定义属性）
                throw IllegalArgumentException("viewPager2’s  parent layout needs to be ConstraintLayout.or you can use indicator in your xml")
            }
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.mBindIndicator = if (smartIndicator == SmartIndicator.CIRCLE) CircleIndicator(mViewPager2.context) else LineIndicator(mViewPager2.context)
                (mViewPager2.parent as ConstraintLayout).addView(this.smartInfo.mBindIndicator as View)
                var layoutParams = (this.smartInfo.mBindIndicator as View).layoutParams as ConstraintLayout.LayoutParams

                when (smartGravity) {
                    SmartGravity.CENTER_HORIZONTAL_BOTTOM -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        if (smartIndicator == SmartIndicator.LINE) {
                            layoutParams.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            layoutParams.leftMargin = mViewPager2.context.resources.getDimension(R.dimen.default_space_line).toInt()
                            layoutParams.rightMargin = mViewPager2.context.resources.getDimension(R.dimen.default_space_line).toInt()
                        }

                        layoutParams.bottomMargin = if (smartIndicator == SmartIndicator.CIRCLE) verticalMargin else mViewPager2.context.resources.getDimension(R.dimen.default_bottom_margin_line).toInt()
                    }

                    SmartGravity.CENTER_HORIZONTAL_TOP -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                    }

                    SmartGravity.LEFT_TOP -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.LEFT_BOTTOM -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.bottomMargin = verticalMargin
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.LEFT_CENTER_VERTICAL -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_TOP -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                        layoutParams.rightMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_BOTTOM -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.bottomMargin = verticalMargin
                        layoutParams.rightMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_CENTER_VERTICAL -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.rightMargin = horizontalMargin
                    }
                }
                (this.smartInfo.mBindIndicator as View).layoutParams = layoutParams
            }
        }
    }


    /**
     * *********************************************************************************************
     * 不需要数据源的构造器
     * */
    class NoDataBuilder {
        private val smartInfo: SmartInfo = SmartInfo()
        private var context: Context? = null

        constructor(fragmentActivity: FragmentActivity) {
            this.smartInfo.fragmentManager = fragmentActivity.supportFragmentManager
            this.smartInfo.lifecycle = fragmentActivity.lifecycle
            this.context = fragmentActivity
        }

        constructor(fragment: Fragment) {
            this.smartInfo.fragmentManager = fragment.childFragmentManager
            this.smartInfo.lifecycle = fragment.lifecycle
            this.context = fragment.requireContext()
        }

        fun overScrollNever(): NoDataBuilder {
            this.smartInfo.isOverScrollNever = true
            return this
        }

        fun canScroll(enableScroll: Boolean): NoDataBuilder {
            this.smartInfo.enableScroll = enableScroll
            return this
        }


        fun asGallery(leftMargin: Int, rightMargin: Int): NoDataBuilder {
            this.smartInfo.isGallery = true
            this.smartInfo.mLeftMargin = leftMargin
            this.smartInfo.mRightMargin = rightMargin
            return this
        }

        fun setPagerTransformer(smartTransformer: SmartTransformer): NoDataBuilder {
            this.smartInfo.smartTransformer = smartTransformer
            return this
        }

        fun withIndicator(smartIndicator: SmartIndicator = SmartIndicator.CIRCLE, smartGravity: SmartGravity = SmartGravity.CENTER_HORIZONTAL_BOTTOM, horizontalMargin: Int = context!!.resources.getDimension(R.dimen.default_bottom_margin).toInt(), verticalMargin: Int = context!!.resources.getDimension(R.dimen.default_bottom_margin).toInt()): NoDataBuilder {
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.isIndicatorFlag = 1
                this.smartInfo.smartIndicator = smartIndicator
                this.smartInfo.smartGravity = smartGravity
                this.smartInfo.horizontalMargin = horizontalMargin
                this.smartInfo.verticalMargin = verticalMargin
            }
            return this
        }

        fun withIndicator(mBindIndicator: BaseIndicator): NoDataBuilder {
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.isIndicatorFlag = 0
                this.smartInfo.mBindIndicator = mBindIndicator
            }
            return this
        }

        fun setVertical(isVertical: Boolean): NoDataBuilder {
            this.smartInfo.isVerticalFlag = if (isVertical) {
                0
            } else {
                1
            }
            return this
        }

        fun bindViews(vararg views: View): NoDataBuilder {
            if (this.smartInfo.mViewList.isNullOrEmpty()) {
                this.smartInfo.mViewList = mutableListOf()
                this.smartInfo.mViewList!!.addAll(views)
            }
            return this
        }

        fun smoothScroll(smoothScroll: Boolean): NoDataBuilder {
            this.smartInfo.smoothScroll = smoothScroll
            return this
        }

        public fun build(viewPager2: ViewPager2): SmartNoDataAdapter {
            if (this.smartInfo.isVerticalFlag != -1) {
                viewPager2.orientation = if (this.smartInfo.isVerticalFlag == 0) {
                    ViewPager2.ORIENTATION_VERTICAL
                } else {
                    ViewPager2.ORIENTATION_HORIZONTAL
                }
            }

            if (this.smartInfo.isGallery) {
                var recycleView = ViewPager2Util.getRecycleFromViewPager2(viewPager2)
                if (viewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                    recycleView?.setPadding(this.smartInfo.mLeftMargin, 0, this.smartInfo.mRightMargin, 0)
                } else {
                    recycleView?.setPadding(0, this.smartInfo.mLeftMargin, 0, this.smartInfo.mRightMargin)
                }
                recycleView?.clipToPadding = false
            }

            if (this.smartInfo.isIndicatorFlag == 1) {
                createCircleIndicator(this.smartInfo.smartIndicator, this.smartInfo.smartGravity, this.smartInfo.horizontalMargin, this.smartInfo.verticalMargin, viewPager2)
            }
            return SmartNoDataAdapter(this.smartInfo, viewPager2)
        }


        private fun createCircleIndicator(smartIndicator: SmartIndicator, smartGravity: SmartGravity, horizontalMargin: Int, verticalMargin: Int, mViewPager2: ViewPager2) {
            //因为不是自定义view,利用viewPager2的父布局去添加指示器，此api需要viewPager2的父布局为ConstraintLayout
            if (mViewPager2.parent !is ConstraintLayout) {
                //使用者viewPager2的父布局不是ConstraintLayout，可以在xml里使用具体demo有演示（这种方式，可以使用指示器所有自定义属性）
                throw IllegalArgumentException("viewPager2’s  parent layout needs to be ConstraintLayout.or you can use indicator in your xml")
            }
            if (this.smartInfo.mBindIndicator == null) {
                this.smartInfo.mBindIndicator = if (smartIndicator == SmartIndicator.CIRCLE) CircleIndicator(mViewPager2.context) else LineIndicator(mViewPager2.context)
                (mViewPager2.parent as ConstraintLayout).addView(this.smartInfo.mBindIndicator as View)
                var layoutParams = (this.smartInfo.mBindIndicator as View).layoutParams as ConstraintLayout.LayoutParams

                when (smartGravity) {
                    SmartGravity.CENTER_HORIZONTAL_BOTTOM -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        if (smartIndicator == SmartIndicator.LINE) {
                            layoutParams.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            layoutParams.leftMargin = mViewPager2.context.resources.getDimension(R.dimen.default_space_line).toInt()
                            layoutParams.rightMargin = mViewPager2.context.resources.getDimension(R.dimen.default_space_line).toInt()
                        }

                        layoutParams.bottomMargin = if (smartIndicator == SmartIndicator.CIRCLE) verticalMargin else mViewPager2.context.resources.getDimension(R.dimen.default_bottom_margin_line).toInt()
                    }

                    SmartGravity.CENTER_HORIZONTAL_TOP -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                    }

                    SmartGravity.LEFT_TOP -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.LEFT_BOTTOM -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.bottomMargin = verticalMargin
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.LEFT_CENTER_VERTICAL -> {
                        layoutParams.leftToLeft = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.leftMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_TOP -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.topMargin = verticalMargin
                        layoutParams.rightMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_BOTTOM -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.bottomMargin = verticalMargin
                        layoutParams.rightMargin = horizontalMargin
                    }

                    SmartGravity.RIGHT_CENTER_VERTICAL -> {
                        layoutParams.rightToRight = mViewPager2.id
                        layoutParams.bottomToBottom = mViewPager2.id
                        layoutParams.topToTop = mViewPager2.id
                        layoutParams.rightMargin = horizontalMargin
                    }
                }
                (this.smartInfo.mBindIndicator as View).layoutParams = layoutParams
            }
        }

    }

}