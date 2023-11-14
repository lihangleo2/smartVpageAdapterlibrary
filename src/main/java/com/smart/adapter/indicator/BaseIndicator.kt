package com.smart.adapter.indicator

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2023/10/23
 */
interface BaseIndicator {
    //ViewPager2滑动监听
    fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)

    fun onPageSelected(position: Int)

    fun onPageScrollStateChanged(state: Int)

    //当数量有变化时，需要调用刷新方法
    fun notifyDataSetChanged()

    fun setTotalCount(totalCount: Int)

    fun setCurrentIndex(currentIndex: Int)

}