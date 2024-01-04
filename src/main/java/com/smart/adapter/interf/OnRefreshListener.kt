package com.smart.adapter.interf

import androidx.annotation.NonNull
import com.smart.adapter.SmartViewPager2Adapter

/**
 * @Author leo
 * @Date 2023/9/6
 */
interface OnRefreshListener {
    fun onRefresh(@NonNull smartAdapter: SmartViewPager2Adapter<*>)
}