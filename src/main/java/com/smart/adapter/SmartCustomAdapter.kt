package com.smart.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2024/3/14
 * 不需要数据源，传入真实的Fragment实例即可
 */
class SmartCustomAdapter : FragmentStateAdapter {
    private val mFragmentList = mutableListOf<Fragment>()

    constructor(fragmentActivity: FragmentActivity) : super(fragmentActivity)

    constructor(fragment: Fragment) : super(fragment){
        //
    }

    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(fragmentManager, lifecycle)

    fun setFragmentList(list: List<Fragment>) {
        mFragmentList.clear()
        mFragmentList.addAll(list)
        notifyItemRangeChanged(0, mFragmentList.size)
    }

    fun setFragmentList(vararg fragments: Fragment) {
        mFragmentList.clear()
        mFragmentList.addAll(fragments)
        notifyItemRangeChanged(0, mFragmentList.size)
    }


    fun getFragment(position: Int): Fragment {
        return mFragmentList[position]
    }

    override fun createFragment(position: Int) = mFragmentList[position]

    override fun getItemCount() = mFragmentList.size

}