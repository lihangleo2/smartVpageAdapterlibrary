package com.smart.adapter.indicator

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.smart.adapter.R


/**
 * @Author leo
 * @Address https://github.com/lihangleo2
 * @Date 2023/10/23
 */
class CircleIndicator : View, BaseIndicator {
    private var mRadius = 0f

    //-1为白色
    private var mSelectColor = -1
    private var mUnSelectColor = -1
    private var mSpace = 0f
    private var mMode = 1
    private var mStrokeWidth = 0f;

    //
    //CODE_LEO 0 -1,
    private var mTotalCount = 0
    private var mCurrentIndex = -1
    private var mScrollWithViewPager2 = true

    private lateinit var mSelectPaint: Paint
    private lateinit var mUnSelectPaint: Paint

    //指示器中心点之间的距离
    private var mIndicatorPointSpace = 0f
    private var mPositionOffset = 0f


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttrs(attrs)
        initPaint()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleIndicator)
        mRadius = typedArray.getDimension(R.styleable.CircleIndicator_lh_indicator_radius, resources.getDimension(R.dimen.default_radius))
        mStrokeWidth = typedArray.getDimension(R.styleable.CircleIndicator_lh_indicator_strokeWidth, resources.getDimension(R.dimen.default_stroke_width))
        mSelectColor = typedArray.getColor(R.styleable.CircleIndicator_lh_indicator_selectColor, ResourcesCompat.getColor(resources, R.color.default_indicator_selcolor, null))
        mSpace = typedArray.getDimension(R.styleable.CircleIndicator_lh_indicator_space, resources.getDimension(R.dimen.default_space))
        mMode = typedArray.getInt(R.styleable.CircleIndicator_lh_indicator_mode, 2)
        mScrollWithViewPager2 = typedArray.getBoolean(R.styleable.CircleIndicator_lh_indicator_scrollWithViewPager2, true)
        mIndicatorPointSpace = mRadius * 2 + mSpace
        if (mRadius - mStrokeWidth / 2 <= 0) {
            //边框模式下，设置的strokeWidth过大时，主动切换的填充模式
            mMode = 1
        }

        mUnSelectColor = typedArray.getColor(R.styleable.CircleIndicator_lh_indicator_unselectColor, ResourcesCompat.getColor(resources, if (mMode == 1) R.color.default_indicator_unselcolor else R.color.default_indicator_selcolor, null))
    }

    private fun initPaint() {
        mSelectPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = mSelectColor
        }

        mUnSelectPaint = Paint().apply {
            style = if (mMode == 1) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = mStrokeWidth
            isAntiAlias = true
            color = mUnSelectColor
        }
    }

    override fun setTotalCount(totalCount: Int) {
        this.mTotalCount = totalCount
    }

    override fun setCurrentIndex(currentIndex: Int) {
        this.mCurrentIndex = if (mTotalCount != 0) currentIndex % mTotalCount else currentIndex
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(), measureHeight())
    }

    private fun measureWidth(): Int {
        return (mTotalCount * mRadius * 2 + (mTotalCount - 1) * mSpace + paddingLeft + paddingRight).toInt()
    }

    private fun measureHeight(): Int {
        return (mRadius * 2 + paddingTop + paddingBottom).toInt()
    }


    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (mTotalCount > 1) {
                drawUnSelIndicator(it)
                drawSelIndicator(it)
            }
        }
    }


    private fun drawSelIndicator(canvas: Canvas) {
//        canvas.drawRoundRect(getUnSelIndicatorX(mCurrentIndex)-mRadius,0f,getSelIndicatorX(mCurrentIndex)+mRadius,height.toFloat(),(height/2).toFloat(),(height/2).toFloat(),mSelectPaint)
        canvas.drawCircle(getSelIndicatorX(mCurrentIndex), (height / 2).toFloat(), mRadius, mSelectPaint)
    }

    private fun drawUnSelIndicator(canvas: Canvas) {
        for (i in 0..mTotalCount) {
            canvas.drawCircle(getUnSelIndicatorX(i), (height / 2).toFloat(), mRadius - mStrokeWidth / 2, mUnSelectPaint)
        }
    }

    private fun getUnSelIndicatorX(index: Int): Float {
        return mRadius + mIndicatorPointSpace * index
    }

    private fun getSelIndicatorX(index: Int): Float {
        return mRadius + mIndicatorPointSpace * index + mPositionOffset * mIndicatorPointSpace
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
        setMeasuredDimension(measureWidth(), measureHeight())
        postInvalidate()
    }


}