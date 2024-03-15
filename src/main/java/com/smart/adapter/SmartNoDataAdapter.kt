package com.smart.adapter

import android.util.Log
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
    private val mFragmentList = mutableListOf<Fragment>()

    internal constructor(smartInfo: SmartInfo, viewPager2: ViewPager2) : super(smartInfo.fragmentManager!!, smartInfo.lifecycle!!) {
        this.smartInfo = smartInfo
        this.mViewPager2 = viewPager2
        initSmartViewPager()
    }


    fun setFragmentList(list: List<Fragment>): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(list)
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }

    fun setFragmentList(vararg fragments: Fragment): SmartNoDataAdapter {
        mFragmentList.clear()
        mFragmentList.addAll(fragments)
        notifyItemRangeChanged(0, mFragmentList.size)
        return this
    }


    fun getFragment(position: Int): Fragment {
        return mFragmentList[position]
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
            indicator?.setTotalCount(mFragmentList.size)
            indicator?.setCurrentIndex(mViewPager2.currentItem)
            indicator?.notifyDataSetChanged()
        }
    }

}