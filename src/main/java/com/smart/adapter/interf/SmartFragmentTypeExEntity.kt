package com.smart.adapter.interf

/**
 * @Author leo
 * @Date 2023/9/6
 */
abstract class SmartFragmentTypeExEntity : SmartFragmentTypeCallback {
    //用以给SmartViewPager的唯一标识
    public var smartViewPagerId = 0L
}

private interface SmartFragmentTypeCallback {
    //根据实体类拿到fragment的type
    fun getFragmentType(): Int
}