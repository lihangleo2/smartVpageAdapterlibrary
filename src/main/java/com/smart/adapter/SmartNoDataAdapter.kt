package com.smart.adapter

import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.smart.adapter.indicator.BaseIndicator
import com.smart.adapter.info.SmartInfo
import com.smart.adapter.transformer.SmartTransformer
import com.smart.adapter.transformer.StereoPagerTransformer
import com.smart.adapter.transformer.StereoPagerVerticalTransformer
import com.smart.adapter.transformer.TransAlphScaleFormer
import com.smart.adapter.util.ViewPager2Util

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2024/3/14
 * 不需要数据源，可传入Fragment对象
 */
class SmartNoDataAdapter : FragmentStateAdapter {
    private lateinit var smartInfo: SmartInfo
    private lateinit var mViewPager2: ViewPager2
    public val mFragmentList = mutableListOf<Fragment>()
    private val mViewList = mutableListOf<View>()
    private var mListener: OnClickListener? = null

    internal constructor(smartInfo: SmartInfo, viewPager2: ViewPager2) : super(smartInfo.fragmentManager!!, smartInfo.lifecycle!!) {
        this.smartInfo = smartInfo
        this.mViewPager2 = viewPager2
        initSmartViewPager()
    }


    @Deprecated("replace addData()")
    fun setFragmentList(list: List<Fragment>): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(list)
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }

    fun addData(list: List<Fragment>): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(list)
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }

    @Deprecated("replace addData()")
    fun setFragmentList(vararg fragments: Fragment): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(fragments)
        mViewPager2.offscreenPageLimit = mFragmentList.size
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }

    fun addData(vararg fragments: Fragment): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(fragments)
        mViewPager2.offscreenPageLimit = mFragmentList.size
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }

    fun getFragment(position: Int): Fragment {
        return mFragmentList[position]
    }

    /**
     * 此api,只允许fragments集合里每个fragment类型都唯一
     * */
    inline fun <reified T> getFragment(): T? {
        var targetFragment: T? = null
        mFragmentList.forEachIndexed { index, fragment ->
            if (fragment is T) {
                if (targetFragment == null) {
                    targetFragment = fragment
                } else {
                    throw IllegalArgumentException("此api只适用fragments类型唯一，泛型存在多个实例")
                }
            }
        }
        return targetFragment
    }

    override fun createFragment(position: Int) = mFragmentList[position]

    override fun getItemCount() = mFragmentList.size


    /**
     * 初始化的一些操作
     * */
    fun initSmartViewPager() {
        if (mViewPager2 == null) {
            throw IllegalArgumentException("the bindView viewPager2 can not be null")
        }

        if (this.smartInfo.isOverScrollNever) {
            ViewPager2Util.cancleViewPagerShadow(mViewPager2)
        }

        canScroll(this.smartInfo.enableScroll)

        if (this.smartInfo.smartTransformer != null) {
            setPagerTransformer(this.smartInfo.smartTransformer!!)
        }

        mViewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
                smartInfo.mBindIndicator?.onPageScrollStateChanged(state)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                smartInfo.mBindIndicator?.onPageSelected(position)
                selectView(position)
            }
        })

        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()

            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                notifyDataSetIndicator(smartInfo.mBindIndicator)
            }

        })
        initViewClickListener()
        cancleSaveEnabled()
    }

    fun canScroll(enableScroll: Boolean): SmartNoDataAdapter {
        mViewPager2.isUserInputEnabled = enableScroll
        return this
    }

    private fun cancleSaveEnabled() {
        mViewPager2.isSaveEnabled = false
    }

    fun setPagerTransformer(smartTransformer: SmartTransformer): SmartNoDataAdapter {
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

    private fun notifyDataSetIndicator(indicator: BaseIndicator?) {
        indicator?.let {
            indicator.setTotalCount(mFragmentList.size)
            indicator.setCurrentIndex(mViewPager2.currentItem)
            indicator.notifyDataSetChanged()
        }
    }

    private fun selectView(position: Int) {
        if (!smartInfo.mViewList.isNullOrEmpty() && position <= smartInfo.mViewList!!.size - 1) {//防止越界报错
            smartInfo.mViewList!!.forEachIndexed { index, view ->
                view.isSelected = index == position
            }
        }
    }

    private fun initViewClickListener() {
        smartInfo.mViewList?.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (mViewPager2.currentItem != index) {
                    if (mListener == null) {
                        mViewPager2.setCurrentItem(index, smartInfo.smoothScroll)
                    } else {
                        if (mListener!!.onClick(view)) {
                            mViewPager2.setCurrentItem(index, smartInfo.smoothScroll)
                        }
                    }
                }
            }
        }
    }

    interface OnClickListener {
        fun onClick(v: View): Boolean
    }

    fun setOnClickListener(listener: OnClickListener): SmartNoDataAdapter {
        this.mListener = listener
        initViewClickListener()
        return this
    }

}