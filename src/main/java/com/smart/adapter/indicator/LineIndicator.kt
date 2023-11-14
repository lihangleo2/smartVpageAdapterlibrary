package com.smart.adapter.indicator

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.smart.adapter.R
import com.smart.adapter.util.ScreenUtils.getScreenWidth

/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2023/11/13
 */
class LineIndicator : View, BaseIndicator {

    //-1为白色
    private var mSelectColor = -1
    private var mUnSelectColor = -1
    private var mSpace = 0f

    //
    private var mTotalCount = 0
    private var mCurrentIndex = -1
    private var mScrollWithViewPager2 = true

    //
    private lateinit var mSelectPaint: Paint
    private lateinit var mUnSelectPaint: Paint

    //
    //指示器中心点之间的距离
    private var mIndicatorWidth = 0f
    private var mPositionOffset = 0f


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttrs(attrs)
        initPaint()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.LineIndicator)
        mSelectColor = typedArray.getColor(R.styleable.LineIndicator_line_indicator_selectColor, ResourcesCompat.getColor(resources, R.color.default_indicator_selcolor, null))
        mUnSelectColor = typedArray.getColor(R.styleable.LineIndicator_line_indicator_unselectColor, ResourcesCompat.getColor(resources, R.color.default_indicator_unselcolor_line, null))
        mSpace = typedArray.getDimension(R.styleable.LineIndicator_line_indicator_space, resources.getDimension(R.dimen.default_space_line))
        mScrollWithViewPager2 = typedArray.getBoolean(R.styleable.LineIndicator_line_indicator_scrollWithViewPager2, true)
    }


    private fun initPaint() {
        mSelectPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = mSelectColor
        }

        mUnSelectPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = mUnSelectColor
        }
    }

    private var mWidthMeasureSpec: Int = MeasureSpec.AT_MOST
    private var mHeightMeasureSpec: Int = MeasureSpec.AT_MOST
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        this.mWidthMeasureSpec = widthMeasureSpec
        this.mHeightMeasureSpec = heightMeasureSpec
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var result = 0
        when (mode) {
            MeasureSpec.EXACTLY -> result = width
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> result = getScreenWidth(context)
            else -> {}
        }


        //计算指示器宽度
        mIndicatorWidth = (result - (mTotalCount - 1) * mSpace) / mTotalCount
        return result
    }

    private fun measureHeight(heightMeasureSpec: Int): Int {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        var result = 0
        when (mode) {
            MeasureSpec.EXACTLY -> result = height
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> result = resources.getDimension(R.dimen.default_line_indicator_height).toInt()
            else -> {}
        }
        return result
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (mTotalCount > 1) {
                drawUnSelIndicator(it)
                drawSelIndicator(it)
                Log.e("没有触发这里的嘛", "线性指示器内部 ---- 开始绘画了--总计${mTotalCount}--当前index ==${mCurrentIndex}")
            }
        }
    }

    private fun drawSelIndicator(canvas: Canvas) {
        var left = getSelIndicatorX(mCurrentIndex)
        Log.e("没有触发这里的嘛", "选中样式的 $left 指示器长度$mIndicatorWidth 指示器高度$height")
        canvas.drawRoundRect(left, 0f, left + mIndicatorWidth, height.toFloat(), height.toFloat() / 2, height.toFloat() / 2, mSelectPaint)
    }

    private fun drawUnSelIndicator(canvas: Canvas) {
        for (i in 0..mTotalCount) {
            var left = getUnSelIndicatorX(i)
            canvas.drawRoundRect(left, 0f, left + mIndicatorWidth, height.toFloat(), height.toFloat() / 2, height.toFloat() / 2, mUnSelectPaint)
        }
    }

    private fun getUnSelIndicatorX(index: Int): Float {
        return (mIndicatorWidth + mSpace) * index
    }

    private fun getSelIndicatorX(index: Int): Float {
        return (mIndicatorWidth + mSpace) * index + mPositionOffset * (mIndicatorWidth + mSpace)
    }


    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (mScrollWithViewPager2) {
            mPositionOffset = positionOffset
            mCurrentIndex = if (mTotalCount != 0) position % mTotalCount else position
            postInvalidate()
        }
    }

    override fun onPageSelected(position: Int) {
        if (mCurrentIndex != position) {
            mCurrentIndex = if (mTotalCount != 0) position % mTotalCount else position
            postInvalidate()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun notifyDataSetChanged() {
        Log.e("没有触发这里的嘛", "线性指示器内部")

        setMeasuredDimension(measureWidth(mWidthMeasureSpec), measureHeight(mHeightMeasureSpec))
        postInvalidate()
    }

    override fun setTotalCount(totalCount: Int) {
        this.mTotalCount = totalCount
    }


    override fun setCurrentIndex(currentIndex: Int) {
        this.mCurrentIndex = if (mTotalCount != 0) currentIndex % mTotalCount else currentIndex
    }
}