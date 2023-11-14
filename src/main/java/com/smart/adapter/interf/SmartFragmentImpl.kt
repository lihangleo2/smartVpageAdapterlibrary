package com.smart.adapter.interf

import java.lang.Deprecated

/**
 * @Author leo
 * @Date 2023/9/7
 * 必须要实现此接口否则报错
 */
@Deprecated
interface SmartFragmentImpl {
    @Deprecated
    fun initSmartFragmentData(bean: SmartFragmentTypeExEntity)
}

interface SmartFragmentImpl2<T:SmartFragmentTypeExEntity> {
    fun initSmartFragmentData(bean: T)
}