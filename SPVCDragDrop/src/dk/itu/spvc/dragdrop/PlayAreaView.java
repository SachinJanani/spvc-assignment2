package dk.itu.spvc.dragdrop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * A view that displays a single image.
 * Methods are defined to let the image be dragged around
 * @author frza
 *
 */
public class PlayAreaView extends View {
	static final String DEBUG_TAG = "PLAYAREA_VIEW";

	/**
	 * Transformation matrix to store the image translation
	 */
	private Matrix translate;
	/**
	 * The image itself
	 */
	private Bitmap image;
	/**
	 * gesture detector
	 */
	private GestureDetector gestures;

	/**
	 * Construct the PlayAreaView
	 * @param context
	 * @param image
	 * @param initX
	 * @param initY
	 */
	PlayAreaView(DragDropActivity context, Bitmap image, float initX, float initY) {
		super(context);
		
		translate = new Matrix();
		this.onMove(initX, initY);
		this.image = image;
		
		//initialize the gestures field with a new ImageDraggedDetector(context,this)
		this.gestures = new GestureDetector(new ImageDraggedDetector(context,this));
		
		//set the context.frame onTouchListener to call this gestures.onTouchEvent(MotionEvent event)
		context.frame.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestures.onTouchEvent(event);
			}
		});
	}
	
	PlayAreaView(DragDropActivity context, int id, float initX, float initY) {
		this(context,BitmapFactory.decodeResource(context.getResources(), id),initX,initY);
	}

	/**
	 * When drawing, use the canvas object to draw the "image" bitmap, using "translate" as the transformation matrix
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(image, translate, null);
	}

	/**
	 * Check that the point identified by x and y is inside the image bounding box.
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isTouched(float x, float y) {
		//# implement me!
		/*
		 * the implementation is as follow:
		 * create a 0,0 point (e.g.: float[] v = {0,0};),
		 * and use the "translate" matrix to map the point (translate.mapPoints(v)).
		 * Then get the image width and height, and check that the:
		 * 	the X parameter coord:
		 * 		is greater than the translated x coord (e.g. v[0])
		 * 		is lower than the translated x coord + the width of the image AND
		 * 	AND the Y parameter coord:
		 * 		is greated than the translated y coord (e.g. v[1]) AND
		 * 		is lower than the translated y coord + the height of the image
		 * */
		float[] v = {0,0};
		translate.mapPoints(v);
		int width = image.getWidth();
		int height = image.getHeight();
		if((x > v[0] && x < (v[0] + width)) && (y > v[1] && y < (y + height))) return true;
		else return false;
	}

	/**
	 * Called by the ImageDraggedDetector when a user scrolled on the image.
	 * Call translate.postTranslate(dx,dy) and invalidate the view (to force a redraw)
	 * @param dx
	 * @param dy
	 */
	public void onMove(float dx, float dy) {
		translate.postTranslate(dx, dy);
		invalidate();
	}
}