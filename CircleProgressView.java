package com.example.myapplication.circleprogress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.myapplication.R;

/**
 * An circle view, similar to Android's ProgressBar.
 * Can be used in 'value mode'
 * In value mode it can be used as a progress bar or to visualize any other value.
 * Setting a value is fully animated.
 * <p/>
 *
 * @author Shijen, based on the CircleProgressView of Jakob Grabner
 * https://github.com/jakob-grabner/Circle-Progress-View
 * <p/>
 * Licensed under the Creative Commons Attribution 3.0 license see:
 * http://creativecommons.org/licenses/by/3.0/
 */
@SuppressWarnings("unused")
public class CircleProgressView extends View implements ValueAnimator.AnimatorUpdateListener {

    /**
     * The log tag.
     */
    private final static String TAG = "CircleView";
    //----------------------------------
    //region members
    //Colors (with defaults)
    private final int mBarColorStandard = 0xff009688; //stylish blue
    protected int mLayoutHeight = 0;
    protected int mLayoutWidth = 0;
    //Rectangles
    protected RectF mCircleBounds = new RectF();
    protected RectF mInnerCircleBound = new RectF();
    protected PointF mCenter;
    //value animation
    Direction mDirection = Direction.CW;
    float mCurrentValue = 0;
    float mValueTo = 0;
    float mValueFrom = 0;
    float mMaxValue = 100;
    float mMinValueAllowed = 0;
    float mMaxValueAllowed = -1;
    //The amount of degree to move the bar by on each draw
    /**
     * The animation duration in ms
     */
    long mAnimationDuration = 900;

    private int mBarWidth = 40;
    private int mRimWidth = 40;
    private int mStartAngle = 270;
    private int mBackgroundCircleColor = 0x00000000;  //transparent
    private int mRimColor = 0xAA83d0c9;

    private int[] mBarColors = new int[]{
            mBarColorStandard //stylish blue
    };
    //Caps
    private Paint.Cap mBarStrokeCap = Paint.Cap.ROUND;
    //Paints
    private Paint mBarPaint = new Paint();
    private Paint mShaderlessBarPaint;
    private Paint mBackgroundCirclePaint = new Paint();
    private Paint mRimPaint = new Paint();

    private boolean mShowBlock = false;
    private int mBlockCount = 18;
    private float mBlockScale = 0.9f;
    private float mBlockDegree = 360 / mBlockCount;
    private float mBlockScaleDegree = mBlockDegree * mBlockScale;
    private boolean mRoundToBlock = false;
    private boolean mRoundToWholeNumber = false;
    private AnimationFinishListener animationFinishListener;

    private ValueAnimator progressAnimator;

    //endregion members
    //----------------------------------

    /**
     * The constructor for the CircleView
     *
     * @param context The context.
     * @param attrs   The attributes.
     */
    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(context.obtainStyledAttributes(attrs,
                R.styleable.CircleProgressView));

        if (!isInEditMode()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
        setupPaints();
    }

    private static float calcTextSizeForRect(String _text, Paint _textPaint, RectF _rectBounds) {

        Matrix matrix = new Matrix();
        Rect textBoundsTmp = new Rect();
        //replace ones because for some fonts the 1 takes less space which causes issues
        String text = _text.replace('1', '0');

        //get current mText bounds
        _textPaint.getTextBounds(text, 0, text.length(), textBoundsTmp);

        RectF textBoundsTmpF = new RectF(textBoundsTmp);

        matrix.setRectToRect(textBoundsTmpF, _rectBounds, Matrix.ScaleToFit.CENTER);
        float values[] = new float[9];
        matrix.getValues(values);
        return _textPaint.getTextSize() * values[Matrix.MSCALE_X];
    }

    /**
     * @param _angle The angle in degree to normalize
     * @return the angle between 0 (EAST) and 360
     */
    private static float normalizeAngle(float _angle) {
        return (((_angle % 360) + 360) % 360);
    }

    /**
     * Calculates the angle from centerPt to targetPt in degrees.
     * The return should range from [0,360), rotating CLOCKWISE,
     * 0 and 360 degrees represents EAST,
     * 90 degrees represents SOUTH, etc...
     * <p/>
     * Assumes all points are in the same coordinate space.  If they are not,
     * you will need to call SwingUtilities.convertPointToScreen or equivalent
     * on all arguments before passing them  to this function.
     *
     * @param centerPt Point we are rotating around.
     * @param targetPt Point we want to calculate the angle to.
     * @return angle in degrees.  This is the angle from centerPt to targetPt.
     */
    public static double calcRotationAngleInDegrees(PointF centerPt, PointF targetPt) {
        // calculate the angle theta from the deltaY and deltaX values
        // (atan2 returns radians values from [-PI,PI])
        // 0 currently points EAST.
        // NOTE: By preserving Y and X param order to atan2,  we are expecting
        // a CLOCKWISE angle direction.
        double theta = Math.atan2(targetPt.y - centerPt.y, targetPt.x - centerPt.x);

        // rotate the theta angle clockwise by 90 degrees
        // (this makes 0 point NORTH)
        // NOTE: adding to an angle rotates it clockwise.
        // subtracting would rotate it counter-clockwise
//        theta += Math.PI/2.0;

        // convert from radians to degrees
        // this will give you an angle from [0->270],[-180,0]
        double angle = Math.toDegrees(theta);

        // convert to positive range [0-360)
        // since we want to prevent negative angles, adjust them now.
        // we can assume that atan2 will not return a negative value
        // greater than one partial rotation
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    //----------------------------------
    //region getter/setter

    public int[] getBarColors() {
        return mBarColors;
    }

    public Paint.Cap getBarStrokeCap() {
        return mBarStrokeCap;
    }

    /**
     * @param _barStrokeCap The stroke cap of the progress bar.
     */
    public void setBarStrokeCap(Paint.Cap _barStrokeCap) {
        mBarStrokeCap = _barStrokeCap;
        mBarPaint.setStrokeCap(_barStrokeCap);
        if (mBarStrokeCap != Paint.Cap.BUTT) {
            mShaderlessBarPaint = new Paint(mBarPaint);
            mShaderlessBarPaint.setShader(null);
            mShaderlessBarPaint.setColor(mBarColors[0]);
        }
    }

    public int getBarWidth() {
        return mBarWidth;
    }

    /**
     * @param barWidth The width of the progress bar in pixel.
     */
    public void setBarWidth(@IntRange(from = 0) int barWidth) {
        this.mBarWidth = barWidth;
        mBarPaint.setStrokeWidth(barWidth);
    }

    public int getBlockCount() {
        return mBlockCount;
    }

    public void setBlockCount(int blockCount) {
        if (blockCount > 1) {
            mShowBlock = true;
            mBlockCount = blockCount;
            mBlockDegree = 360.0f / blockCount;
            mBlockScaleDegree = mBlockDegree * mBlockScale;
        } else {
            mShowBlock = false;
        }
    }

    public void setRoundToBlock(boolean _roundToBlock) {
        mRoundToBlock = _roundToBlock;
    }

    public boolean getRoundToBlock() {
        return mRoundToBlock;
    }

    public void setRoundToWholeNumber(boolean roundToWholeNumber) {
        mRoundToWholeNumber = roundToWholeNumber;
    }

    public boolean getRoundToWholeNumber() {
        return mRoundToWholeNumber;
    }

    public float getBlockScale() {
        return mBlockScale;
    }

    public void setBlockScale(@FloatRange(from = 0.0, to = 1) float blockScale) {
        if (blockScale >= 0.0f && blockScale <= 1.0f) {
            mBlockScale = blockScale;
            mBlockScaleDegree = mBlockDegree * blockScale;
        }
    }

    public int getFillColor() {
        return mBackgroundCirclePaint.getColor();
    }

    public float getCurrentValue() {
        return mCurrentValue;
    }

    public float getMinValueAllowed() {
        return mMinValueAllowed;
    }

    public float getMaxValueAllowed() {
        return mMaxValueAllowed;
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    /**
     * The max value of the progress bar. Used to calculate the percentage of the current value.
     * The bar fills according to the percentage. The default value is 100.
     *
     * @param _maxValue The max value.
     */
    public void setMaxValue(@FloatRange(from = 0) float _maxValue) {
        mMaxValue = _maxValue;
    }

    /**
     * The min value allowed of the progress bar. Used to limit the min possible value of the current value.
     *
     * @param _minValueAllowed The min value allowed.
     */
    public void setMinValueAllowed(@FloatRange(from = 0) float _minValueAllowed) {
        mMinValueAllowed = _minValueAllowed;
    }

    /**
     * The max value allowed of the progress bar. Used to limit the max possible value of the current value.
     *
     * @param _maxValueAllowed The max value allowed.
     */
    public void setMaxValueAllowed(@FloatRange(from = 0) float _maxValueAllowed) {
        mMaxValueAllowed = _maxValueAllowed;
    }

    public int getRimColor() {
        return mRimColor;
    }

    /**
     * @param rimColor The color of the rim around the Circle.
     */
    public void setRimColor(@ColorInt int rimColor) {
        mRimColor = rimColor;
        mRimPaint.setColor(rimColor);
    }

    public Shader getRimShader() {
        return mRimPaint.getShader();
    }

    public void setRimShader(Shader shader) {
        this.mRimPaint.setShader(shader);
    }

    public int getRimWidth() {
        return mRimWidth;
    }

    /**
     * @param rimWidth The width in pixel of the rim around the circle
     */
    public void setRimWidth(@IntRange(from = 0) int rimWidth) {
        mRimWidth = rimWidth;
        mRimPaint.setStrokeWidth(rimWidth);
    }

    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(@IntRange(from = 0, to = 360) int _startAngle) {
        // get a angle between 0 and 360
        mStartAngle = (int) normalizeAngle(_startAngle);
    }

    public boolean isShowBlock() {
        return mShowBlock;
    }

    public void setShowBlock(boolean showBlock) {
        mShowBlock = showBlock;
    }

    /**
     * Sets the color of progress bar.
     *
     * @param barColors One or more colors. If more than one color is specified, a gradient of the colors is used.
     */
    public void setBarColor(@ColorInt int... barColors) {
        this.mBarColors = barColors;
        setupBarPaint();
    }

    /**
     * Sets the background color of the entire Progress Circle.
     * Set the color to 0x00000000 (Color.TRANSPARENT) to hide it.
     *
     * @param circleColor the color.
     */
    public void setFillCircleColor(@ColorInt int circleColor) {
        mBackgroundCircleColor = circleColor;
        mBackgroundCirclePaint.setColor(circleColor);
    }

    /**
     * Sets the direction of circular motion (clockwise or counter-clockwise).
     */
    public void setDirection(Direction direction) {
        mDirection = direction;
    }

    /**
     * Set the value of the circle view without an animation.
     * Stops any currently active animations.
     *
     * @param _value The value.
     */
    public void setValue(float _value) {
        // round to block
        if (mShowBlock && mRoundToBlock) {
            float value_per_block = mMaxValue / (float) mBlockCount;
            _value = Math.round(_value / value_per_block) * value_per_block;

        } else if (mRoundToWholeNumber) { // round to whole number
            _value = Math.round(_value);
        }

        // respect min and max values allowed
        _value = Math.max(mMinValueAllowed, _value);

        if (mMaxValueAllowed >= 0)
            _value = Math.min(mMaxValueAllowed, _value);
        mCurrentValue = _value;
        invalidate();
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo value after animation
     */
    public void setValueAnimated(float _valueTo) {
        setValueAnimated(_valueTo, 1200);
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds.
     */
    public void setValueAnimated(float _valueTo, long _animationDuration) {
        setValueAnimated(mCurrentValue, _valueTo, _animationDuration);
    }

    /**
     * Sets the value of the circle view with an animation.
     *
     * @param _valueFrom         start value of the animation
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds
     */
    public void setValueAnimated(float _valueFrom, float _valueTo, long _animationDuration) {
        // round to block
        if (mShowBlock && mRoundToBlock) {
            float value_per_block = mMaxValue / (float) mBlockCount;
            _valueTo = Math.round(_valueTo / value_per_block) * value_per_block;

        } else if (mRoundToWholeNumber) {
            _valueTo = Math.round(_valueTo);
        }

        // respect min and max values allowed
        _valueTo = Math.max(mMinValueAllowed, _valueTo);

        if (mMaxValueAllowed >= 0)
            _valueTo = Math.min(mMaxValueAllowed, _valueTo);
        mValueFrom = _valueFrom;
        mValueTo = _valueTo;
        mAnimationDuration = _animationDuration;
        animateProgress();
    }

    private void animateProgress() {
        progressAnimator = ValueAnimator.ofFloat(mValueFrom, mValueTo);
        progressAnimator.setDuration(mAnimationDuration);
        progressAnimator.addUpdateListener(this);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animationFinishListener != null) {
                    animationFinishListener.onAnimationUpdateListener();
                }
            }
        });
        progressAnimator.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pauseAnimation() {
        if (progressAnimator != null && !progressAnimator.isPaused()) {
            progressAnimator.pause();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void resumeAnimation() {
        if (progressAnimator != null && progressAnimator.isPaused()) {
            progressAnimator.resume();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean isAnimationPaused() {
        if (progressAnimator != null) {
            return progressAnimator.isPaused();
        }
        return false;
    }

    public void setAnimationFinishListener(AnimationFinishListener animationFinishListener) {
        this.animationFinishListener = animationFinishListener;
    }

    //endregion getter/setter
    //----------------------------------


    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        setBarWidth((int) a.getDimension(R.styleable.CircleProgressView_cpv_barWidth,
                mBarWidth));

        setRimWidth((int) a.getDimension(R.styleable.CircleProgressView_cpv_rimWidth,
                mRimWidth));

        setDirection(Direction.values()[a.getInt(R.styleable.CircleProgressView_cpv_direction, 0)]);

        float value = a.getFloat(R.styleable.CircleProgressView_cpv_value, mCurrentValue);
        setValue(value);
        mCurrentValue = value;

        if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor2) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor3)) {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor3, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor2)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard)};

        } else {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard)};
        }

        if (a.hasValue(R.styleable.CircleProgressView_cpv_barStrokeCap)) {
            setBarStrokeCap(StrokeCap.values()[a.getInt(R.styleable.CircleProgressView_cpv_barStrokeCap, 0)].paintCap);
        }

        setRimColor(a.getColor(R.styleable.CircleProgressView_cpv_rimColor,
                mRimColor));

        setFillCircleColor(a.getColor(R.styleable.CircleProgressView_cpv_fillColor,
                mBackgroundCircleColor));

        setMaxValue(a.getFloat(R.styleable.CircleProgressView_cpv_maxValue, mMaxValue));

        setMinValueAllowed(a.getFloat(R.styleable.CircleProgressView_cpv_minValueAllowed, mMinValueAllowed));
        setMaxValueAllowed(a.getFloat(R.styleable.CircleProgressView_cpv_maxValueAllowed, mMaxValueAllowed));

        setRoundToBlock(a.getBoolean(R.styleable.CircleProgressView_cpv_roundToBlock, mRoundToBlock));
        setRoundToWholeNumber(a.getBoolean(R.styleable.CircleProgressView_cpv_roundToWholeNumber, mRoundToWholeNumber));

        setStartAngle(a.getInt(R.styleable.CircleProgressView_cpv_startAngle, mStartAngle));

        if (a.hasValue(R.styleable.CircleProgressView_cpv_blockCount)) {
            setBlockCount(a.getInt(R.styleable.CircleProgressView_cpv_blockCount, 1));
            setBlockScale(a.getFloat(R.styleable.CircleProgressView_cpv_blockScale, 0.9f));
        }

        // Recycle
        a.recycle();
    }

    /*
     * When this is called, make the view square.
     * From: http://www.jayway.com/2012/12/12/creating-custom-android-views-part-4-measuring-and-how-to-force-a-view-to-be-square/
     *
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The first thing that happen is that we call the superclass
        // implementation of onMeasure. The reason for that is that measuring
        // can be quite a complex process and calling the super method is a
        // convenient way to get most of this complexity handled.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We can’t use getWidth() or getHeight() here. During the measuring
        // pass the view has not gotten its final size yet (this happens first
        // at the start of the layout pass) so we have to use getMeasuredWidth()
        // and getMeasuredHeight().
        int size;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();


        // Finally we have some simple logic that calculates the size of the view
        // and calls setMeasuredDimension() to set that size.
        // Before we compare the width and height of the view, we remove the padding,
        // and when we set the dimension we add it back again. Now the actual content
        // of the view will be square, but, depending on the padding, the total dimensions
        // of the view might not be.
        if (widthWithoutPadding > heightWithoutPadding) {
            size = heightWithoutPadding;
        } else {
            size = widthWithoutPadding;
        }

        // If you override onMeasure() you have to call setMeasuredDimension().
        // This is how you report back the measured size.  If you don’t call
        // setMeasuredDimension() the parent will throw an exception and your
        // application will crash.
        // We are calling the onMeasure() method of the superclass so we don’t
        // actually need to call setMeasuredDimension() since that takes care
        // of that. However, the purpose with overriding onMeasure() was to
        // change the default behaviour and to do that we need to call
        // setMeasuredDimension() with our own values.
        setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size + getPaddingTop() + getPaddingBottom());
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT and WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Share the dimensions
        mLayoutWidth = w;
        mLayoutHeight = h;

        setupBounds();
        setupBarPaint();

        invalidate();
    }

    //----------------------------------
    // region helper
    private float calcTextSizeForCircle(String _text, Paint _textPaint, RectF _circleBounds) {

        //get mActualTextBounds bounds
        RectF innerCircleBounds = getInnerCircleRect(_circleBounds);
        return calcTextSizeForRect(_text, _textPaint, innerCircleBounds);

    }

    private RectF getInnerCircleRect(RectF _circleBounds) {

        double circleWidth = +_circleBounds.width() - (Math.max(mBarWidth, mRimWidth));
        double width = ((circleWidth / 2d) * Math.sqrt(2d));
        float widthDelta = (_circleBounds.width() - (float) width) / 2f;

        float scaleX = 1;
        float scaleY = 1;
        return new RectF(_circleBounds.left + (widthDelta * scaleX), _circleBounds.top + (widthDelta * scaleY), _circleBounds.right - (widthDelta * scaleX), _circleBounds.bottom - (widthDelta * scaleY));

    }


    //endregion helper
    //----------------------------------

    //----------------------------------
    //region Setting up stuff

    /**
     * Set the bounds of the component
     */
    private void setupBounds() {
        // Width should equal to Height, find the min value to setup the circle
        int minValue = Math.min(mLayoutWidth, mLayoutHeight);

        // Calc the Offset if needed
        int xOffset = mLayoutWidth - minValue;
        int yOffset = mLayoutHeight - minValue;

        // Add the offset
        float paddingTop = this.getPaddingTop() + (yOffset / 2);
        float paddingBottom = this.getPaddingBottom() + (yOffset / 2);
        float paddingLeft = this.getPaddingLeft() + (xOffset / 2);
        float paddingRight = this.getPaddingRight() + (xOffset / 2);

        int width = getWidth(); //this.getLayoutParams().width;
        int height = getHeight(); //this.getLayoutParams().height;

        float circleWidthHalf = mBarWidth / 2f > mRimWidth / 2f ? mBarWidth / 2f : mRimWidth / 2f;

        mCircleBounds = new RectF(paddingLeft + circleWidthHalf,
                paddingTop + circleWidthHalf,
                width - paddingRight - circleWidthHalf,
                height - paddingBottom - circleWidthHalf);


        mInnerCircleBound = new RectF(paddingLeft + (mBarWidth),
                paddingTop + (mBarWidth),
                width - paddingRight - (mBarWidth),
                height - paddingBottom - (mBarWidth));
        mCenter = new PointF(mCircleBounds.centerX(), mCircleBounds.centerY());
    }

    private void setupBarPaint() {
        if (mBarColors.length > 1) {
            mBarPaint.setShader(new SweepGradient(mCircleBounds.centerX(), mCircleBounds.centerY(), mBarColors, null));
            Matrix matrix = new Matrix();
            mBarPaint.getShader().getLocalMatrix(matrix);
            matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY());

            matrix.postRotate(mStartAngle);
            matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY());
            mBarPaint.getShader().setLocalMatrix(matrix);
            //mBarPaint.setColor(mBarColors[0]);
        } else if (mBarColors.length == 1) {
            mBarPaint.setColor(mBarColors[0]);
            mBarPaint.setShader(null);
        } else {
            mBarPaint.setColor(mBarColorStandard);
            mBarPaint.setShader(null);
        }

        mBarPaint.setAntiAlias(true);
        mBarPaint.setStrokeCap(mBarStrokeCap);
        mBarPaint.setStyle(Style.STROKE);
        mBarPaint.setStrokeWidth(mBarWidth);

        if (mBarStrokeCap != Paint.Cap.BUTT) {
            mShaderlessBarPaint = new Paint(mBarPaint);
            mShaderlessBarPaint.setShader(null);
            mShaderlessBarPaint.setColor(mBarColors[0]);
        }
    }


    /**
     * Setup all paints.
     * Call only if changes to color or size properties are not visible.
     */
    public void setupPaints() {
        setupBarPaint();
        setupBackgroundCirclePaint();
        setupRimPaint();
    }

    private void setupBackgroundCirclePaint() {
        mBackgroundCirclePaint.setColor(mBackgroundCircleColor);
        mBackgroundCirclePaint.setAntiAlias(true);
        mBackgroundCirclePaint.setStyle(Style.FILL);
    }

    private void setupRimPaint() {
        mRimPaint.setColor(mRimColor);
        mRimPaint.setAntiAlias(true);
        mRimPaint.setStyle(Style.STROKE);
        mRimPaint.setStrokeWidth(mRimWidth);
        mRimPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    //endregion Setting up stuff
    //----------------------------------

    //----------------------------------
    //region draw all the things

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float degrees = (360f / mMaxValue * mCurrentValue);

        //Draw the rim
        if (mRimWidth > 0) {
            if (!mShowBlock) {
                canvas.drawArc(mCircleBounds, 360, 360, false, mRimPaint);
            } else {
                drawBlocks(canvas, mCircleBounds, mStartAngle, 360, false, mRimPaint);
            }
        }
        drawBar(canvas, degrees);
    }

    private void drawBlocks(Canvas _canvas, RectF circleBounds, float startAngle, float _degrees, boolean userCenter, Paint paint) {
        float tmpDegree = 0.0f;
        while (tmpDegree < _degrees) {
            _canvas.drawArc(circleBounds, startAngle + tmpDegree, Math.min(mBlockScaleDegree, _degrees - tmpDegree), userCenter, paint);
            tmpDegree += mBlockDegree;
        }
    }

    private void drawBar(Canvas _canvas, float _degrees) {
        float startAngle = mDirection == Direction.CW ? mStartAngle : mStartAngle - _degrees;
        if (!mShowBlock) {

            if (mBarStrokeCap != Paint.Cap.BUTT && _degrees > 0 && mBarColors.length > 1) {
                if (_degrees > 180) {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees / 2, false, mBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle, 1, false, mShaderlessBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle + (_degrees / 2), _degrees / 2, false, mBarPaint);
                } else {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle, 1, false, mShaderlessBarPaint);
                }

            } else {
                _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint);
            }
        } else {
            drawBlocks(_canvas, mCircleBounds, startAngle, _degrees, false, mBarPaint);
        }
    }

    //endregion draw
    //----------------------------------


    //----------------------------------
    //region touch input
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                pauseAnimation();
                break;
            }
            case MotionEvent.ACTION_UP: {
                resumeAnimation();
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private float getRotationAngleForPointFromStart(PointF point) {
        long angle = Math.round(calcRotationAngleInDegrees(mCenter, point));
        float fromStart = mDirection == Direction.CW ? angle - mStartAngle : mStartAngle - angle;
        return normalizeAngle(fromStart);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mCurrentValue = (Float) animation.getAnimatedValue();
        invalidate();
    }

    public interface AnimationFinishListener {
        public void onAnimationUpdateListener();
    }
}