package com.smart.adapter.interf

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2023/10/20
 * 边缘滑动回调
 */
interface onSideListener {
    /**
     * 左边界回调
     */
    fun onLeftSide()

    /**
     * 右边界回调
     */
    fun onRightSide()
}