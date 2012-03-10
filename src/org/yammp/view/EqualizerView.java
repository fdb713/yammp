package org.yammp.view;

import java.util.Arrays;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class EqualizerView extends View implements OnTouchListener {

	private short[] mBands = new short[] {};

	private float firstMultiplier = 0.33f;
	private float secondMultiplier = 1 - firstMultiplier;
	private Point p1 = new Point(), p3 = new Point();
	private int GRID_WH = 64;
	private short MIN_LEVEL = 0, MAX_LEVEL = 0;
	private HashMap<Short, Integer> mCenterFreqs = new HashMap<Short, Integer>();
	private OnBandLevelChangeListener mListener;

	public EqualizerView(Context context) {
		super(context);
		setOnTouchListener(this);
	}

	public EqualizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this);
	}

	@Override
	public void onDraw(Canvas canvas) {

		Point[] points = new Point[mBands.length];

		for (int i = 0; i < mBands.length; i++) {

			if (MIN_LEVEL != 0 && MAX_LEVEL != 0) {
				points[i] = new Point((int) ((float) i / (mBands.length - 1)
						* (getWidth() - GRID_WH) + GRID_WH / 2),
						(int) (getHeight() / 2 - (float) mBands[i]
								/ (Math.abs(MIN_LEVEL) + Math.abs(MAX_LEVEL)) * getHeight()));
			} else {
				points[i] = new Point((int) ((float) i / (mBands.length - 1)
						* (getWidth() - GRID_WH) + GRID_WH / 2), getHeight() / 2);
			}

		}

		drawBackground(canvas, points);
		drawPaths(canvas, points);
		drawBars(canvas, points);
		drawPoints(canvas, points);
		drawLabels(canvas, points);

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		short band = findClosest(event.getX(), event.getY());
		if (mBands == null || mBands.length < 1) return false;
		if (band != -1 && band < mBands.length) {
			short level = (short) ((getHeight() / 2 - event.getY()) / getHeight() * (Math
					.abs(MIN_LEVEL) + Math.abs(MAX_LEVEL)));
			if (level < MIN_LEVEL) {
				level = MIN_LEVEL;
			}
			if (level > MAX_LEVEL) {
				level = MAX_LEVEL;
			}
			if (mListener != null) {
				mListener.onBandLevelChange(band, level);
			}
			setBandLevel(band, level);
		}
		return true;
	}

	public void setBandLevel(short band, short level) {
		if (mBands == null || band < 0 || band >= mBands.length) return;
		mBands[band] = level;
		invalidate();
	}

	public void setBandLevelRange(short[] range) {
		if (range == null || range.length != 2) return;
		MIN_LEVEL = range[0];
		MAX_LEVEL = range[1];
		invalidate();
	}

	public void setCenterFreq(short band, int freq) {
		mCenterFreqs.put(band, freq);
		invalidate();
	}

	public void setNumberOfBands(short bands) {
		mBands = new short[bands];
		Arrays.fill(mBands, (short) 0);
		invalidate();
	}

	public void setOnBandLevelChangeListener(OnBandLevelChangeListener listener) {
		mListener = listener;
	}

	private void calc(Point[] points, Point result, int index1, int index2, final float multiplier) {

		float diffX = points[index2].x - points[index1].x; // p2.x - p1.x;
		float diffY = points[index2].y - points[index1].y; // p2.y - p1.y;
		result.x = (int) (points[index1].x + diffX * multiplier);
		result.y = (int) (points[index1].y + diffY * multiplier);
	}

	private void drawBackground(Canvas canvas, Point[] points) {

		Paint paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.DKGRAY);
			}
		};

		int linesX = getWidth() / GRID_WH;
		int linesY = getHeight() / GRID_WH;

		for (int i = 0; i < linesX; i++) {
			canvas.drawLine(getWidth() * i / linesX, 0, getWidth() * i / linesX, getHeight(), paint);
		}

		for (int i = 0; i < linesY; i++) {
			canvas.drawLine(0, getHeight() * i / linesY, getWidth(), getHeight() * i / linesY,
					paint);
		}

		paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.CYAN);
				setStrokeWidth(1.5f);
			}
		};

		canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, paint);

		if (mBands != null && mBands.length > 0) {

			paint = new TextPaint() {

				{
					setStyle(Paint.Style.STROKE);
					setAntiAlias(true);
					setColor(Color.WHITE);
					setTextSize(15.0f);
				}
			};

			for (short i = 0; i < mBands.length; i++) {

				String label = "";
				Integer value = mCenterFreqs.get(i);
				if (value != null) {

					int freq = value;
					if (freq / 1000 < 1000) {
						label = String.format("%1.1f", (float) freq / 1000) + " Hz";
					} else {
						label = String.format("%1.1f", (float) freq / 1000 / 1000) + " KHz";
					}
					canvas.drawText(label, points[i].x, getHeight(), paint);
				}
			}
		}
	}

	private void drawBars(Canvas canvas, Point[] points) {
		if (points == null || points.length < 1) return;
		Paint paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.GREEN);
				if (mBands == null || mBands.length < 1) {
					setStrokeWidth(12.0f);
				} else {
					setStrokeWidth(getWidth() / mBands.length / 8);
				}
				setShader(new LinearGradient(0, 0, 0, getHeight(), 0xffbfff00, 0xff003300,
						Shader.TileMode.CLAMP));
			}
		};

		for (Point point : points) {
			canvas.drawLine(point.x, getHeight() / 2, point.x, point.y, paint);
		}
	}

	private void drawLabels(Canvas canvas, Point[] points) {
		if (points == null || points.length < 1) return;
		Paint paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.WHITE);
			}
		};

		for (Point point : points) {
			String label = String.format("%1.1f", (float) (getHeight() / 2 - point.y) / getHeight()
					* (Math.abs(MIN_LEVEL) + Math.abs(MAX_LEVEL)) / 100)
					+ " dB";
			canvas.drawText(label, point.x, getHeight() / 2, paint);
		}

	}

	private void drawPaths(Canvas canvas, Point[] points) {
		if (points == null || points.length < 1) return;
		Paint paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.MAGENTA);
			}
		};

		Path p = new Path();
		float x = points[0].x;
		float y = points[0].y;
		p.moveTo(x, y);

		int length = points.length;

		for (int i = 0; i < length; i++) {
			int nextIndex = i + 1 < length ? i + 1 : i;
			int nextNextIndex = i + 2 < length ? i + 2 : nextIndex;
			calc(points, p1, i, nextIndex, secondMultiplier);
			calc(points, p3, nextIndex, nextNextIndex, firstMultiplier);
			p.cubicTo(p1.x, p1.y, points[nextIndex].x, points[nextIndex].y, p3.x, p3.y);
		}
		canvas.drawPath(p, paint);
	}

	private void drawPoints(Canvas canvas, Point[] points) {
		if (points == null || points.length < 1) return;
		Paint paint = new Paint() {

			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setColor(Color.WHITE);
				if (mBands == null || mBands.length < 1) {
					setStrokeWidth(12.0f);
				} else {
					setStrokeWidth(getWidth() / mBands.length / 8);
				}
			}
		};

		for (Point point : points) {
			canvas.drawPoint(point.x, point.y, paint);
		}
	}

	private short findClosest(float x, float y) {
		if (mBands == null || mBands.length < 1) return -1;
		int count = mBands.length;
		for (int i = 0; i < count; i++) {
			int n = (int) (getWidth() * ((float) i / count));
			if (n < x && n + (getWidth() - GRID_WH) / mBands.length > x) return (short) i;
		}
		return -1;
	}

	public interface OnBandLevelChangeListener {

		void onBandLevelChange(short band, short level);
	}
}
