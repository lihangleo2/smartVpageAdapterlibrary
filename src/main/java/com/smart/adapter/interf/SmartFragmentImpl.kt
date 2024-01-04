package com.smart.adapter.interf

import java.lang.Deprecated

/**
 * @Author leo
 * @Date 2023/9/7
 * 必须要实现此接口否则报错
 */
//@Deprecated
interface SmartFragmentImpl<T:SmartFragmentTypeExEntity> {
    fun initSmartFragmentData(bean: T)
}