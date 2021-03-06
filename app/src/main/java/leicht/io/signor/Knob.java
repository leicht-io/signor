package leicht.io.signor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class Knob extends View implements View.OnClickListener, GestureDetector.OnGestureListener {
    private static final int MARGIN = 8;

    private static final float MIN = -400;
    private static final float MAX = 680;
    private static final int SCALE = 50;
    private static final int VELOCITY = 75;

    private int parentWidth;
    private int parentHeight;

    private int width;
    private int height;
    private final int knobBackgroundColor = Color.parseColor(getContext().getString(R.string.knobBackgroundColor));
    private final int knobGradientColor = Color.parseColor(getContext().getString(R.string.knobGradientColor));
    private final int knobDimpleColor1 = Color.parseColor(getContext().getString(R.string.knobDimple1Color));
    private final int knobDimpleColor2 = Color.parseColor(getContext().getString(R.string.knobDimple2Color));

    private boolean move;
    private float value;
    private float last;

    private final Matrix matrix;
    private Paint paint;
    private LinearGradient gradient;
    private LinearGradient dimple;
    private final GestureDetector detector;

    private OnKnobChangeListener listener;

    public Knob(Context context, AttributeSet attrs) {
        super(context, attrs);

        matrix = new Matrix();
        detector = new GestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View parent = (View) getParent();
        int width = parent.getMeasuredWidth();
        int height = parent.getMeasuredHeight();

        if (width > height) {
            if (parentWidth < width) {
                parentWidth = width;
            }

            if (parentHeight < height) {
                parentHeight = height;
            }
        }

        width = (parentWidth - MARGIN) / 2;
        height = (parentWidth - MARGIN) / 2;

        this.setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.width = width;
        this.height = height;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int gradientY0 = -height * 2 / 3;
        int gradientY1 = Math.abs(gradientY0);
        int dimpleX0 = MARGIN / 2;
        int dimpleY0 = -dimpleX0;

        gradient = new LinearGradient(0, gradientY0, 0, gradientY1, knobBackgroundColor, knobGradientColor, Shader.TileMode.CLAMP);
        dimple = new LinearGradient(dimpleX0, dimpleY0, dimpleX0, dimpleX0, knobDimpleColor1, knobDimpleColor2, Shader.TileMode.CLAMP);
    }

    protected float getValue() {
        return value;
    }

    protected void setValue(float value) {
        this.value = value;

        if (listener != null) {
            listener.onKnobChange(this, this.value);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(width / 2, height / 2);

        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);

        int radius = Math.min(width, height) / 2;
        canvas.drawCircle(0, 0, radius, paint);

        paint.setShader(null);
        paint.setColor(knobBackgroundColor);
        canvas.drawCircle(0, 0, radius - MARGIN, paint);

        float x = (float) (Math.sin(value * Math.PI / SCALE) * radius * 0.8);
        float y = (float) (-Math.cos(value * Math.PI / SCALE) * radius * 0.8);

        paint.setShader(dimple);
        matrix.setTranslate(x, y);
        dimple.setLocalMatrix(matrix);
        canvas.drawCircle(x, y, MARGIN, paint);
    }

    @Override
    public void onClick(View v) {
        value = Math.round(value);

        if (listener != null) {
            listener.onKnobChange(this, value);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector != null) {
            detector.onTouchEvent(event);
        }

        float x = event.getX() - width / 2f;
        float y = event.getY() - height / 2f;

        float theta = (float) Math.atan2(x, -y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                if (!move) {
                    move = true;
                } else {
                    float delta = theta - last;

                    if (delta > Math.PI) {
                        delta -= 2.0 * Math.PI;
                    }

                    if (delta < -Math.PI) {
                        delta += 2.0 * Math.PI;
                    }

                    value += delta * SCALE / Math.PI;

                    if (value < MIN) {
                        value = MIN;
                    }

                    if (value > MAX) {
                        value = MAX;
                    }

                    if (listener != null) {
                        listener.onKnobChange(this, value);
                    }

                    invalidate();
                }
                last = theta;
                break;

            case MotionEvent.ACTION_UP:
                move = false;
                break;
        }
        return true;
    }

    private float calculateNewFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        float x1 = event1.getX() - width / 2f;
        float y1 = event1.getY() - height / 2f;
        float x2 = event2.getX() - width / 2f;
        float y2 = event2.getY() - height / 2f;

        float theta1 = (float) Math.atan2(x1, -y1);
        float theta2 = (float) Math.atan2(x2, -y2);

        float delta = theta2 - theta1;
        float velocity = (float) Math.abs(Math.hypot(velocityX, velocityY));

        if (delta > Math.PI) {
            delta -= 2.0 * Math.PI;
        }

        if (delta < -Math.PI) {
            delta += 2.0 * Math.PI;
        }

        return value + Math.signum(delta) * velocity / VELOCITY;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        ValueAnimator animator = ValueAnimator.ofFloat(value, calculateNewFling(event1, event2, velocityX, velocityY));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            value = (Float) animation.getAnimatedValue();

            if (value < MIN) {
                animation.cancel();
                value = MIN;
            }

            if (value > MAX) {
                animation.cancel();
                value = MAX;
            }

            if (listener != null) {
                listener.onKnobChange(this, value);
            }

            invalidate();
        });
        animator.start();

        return true;
    }

    protected void setOnKnobChangeListener(OnKnobChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
