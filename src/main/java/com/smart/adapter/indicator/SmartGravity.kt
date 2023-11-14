package com.smart.adapter.indicator

/**
 * @Author leo123456
 * @Date 2023/9/9
 */
enum class SmartGravity {
    //左边
    LEFT_TOP,
    LEFT_BOTTOM,
    LEFT_CENTER_VERTICAL,//左边上下居中（竖直margin会失效）

    //右边
    RIGHT_TOP,
    RIGHT_BOTTOM,
    RIGHT_CENTER_VERTICAL,//右边上下居中（竖直margin会失效）

    //水平居中靠上（水平margin会失效）
    CENTER_HORIZONTAL_TOP,
    //水平居中靠下（水平margin会失效）
    CENTER_HORIZONTAL_BOTTOM
}