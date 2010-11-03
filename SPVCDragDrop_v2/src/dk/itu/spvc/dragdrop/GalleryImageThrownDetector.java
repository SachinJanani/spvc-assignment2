package dk.itu.spvc.dragdrop;

import android.graphics.Rect;
import android.os.Parcel;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.Gallery;

/**
 * Gesture listener that detects when an user "throws" an image from the gallery
 * to the frame area
 * 
 * @author frza
 * 
 */
public class GalleryImageThrownDetector extends SimpleOnGestureListener {
	static final String TAG = "GALLERY_IMAGE_THROWN_DETECTOR";

	GestureDetector gestures;
	DragDropActivity activity;

	protected GalleryImageThrownDetector(DragDropActivity activity) {
		this.activity = activity;

		// create a "gesture detector" object, using "this" as OnGestureListener
		this.gestures = new GestureDetector(this);

		/*
		 * set the onTouchListener of activity.gallery to call:
		 * gestures.onTouchEvent(MotionEvent evt)
		 */
		// # set the onTouch listener for activity.gallery
		this.activity.gallery.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestures.onTouchEvent(event);
			}

		});
		//
	}

	/**
	 * Returns the image, if any, located at the pointer coordinates
	 * 
	 * @param evt
	 * @return
	 */
	protected FileAwareImageView getImageCurrentlyAt(MotionEvent evt) {
		// get the event x,y coords
		int x, y;
		x = (int) evt.getX();
		y = (int) evt.getY();

		// perform a translation:
		// the motion event coordinates are local to the view where the gesture
		// detector
		// resides, so relative to the gallery object.
		// Get the global visible Rect of the gallery and translate x,y with the
		// rect left and top
		Gallery gallery = activity.gallery;
		int c = gallery.getChildCount();
		Rect gRect = new Rect();
		gallery.getGlobalVisibleRect(gRect);
		x += gRect.left;
		y += gRect.top;

		Rect rect = new Rect();
		// iterate on the gallery children
		for (int i = 0; i < c; i++) {
			// we know they are FileAwareImageView
			FileAwareImageView view = (FileAwareImageView) gallery
					.getChildAt(i);
			// check that the view is visible
			if (view.getGlobalVisibleRect(rect)) {
				Log.d(TAG, "view visible");
				// return the image, if its visible rect contains the
				// coordinates
				if (rect.contains(x, y)) {
					Log.d(TAG, "view " + view + " hit!");
					return view;
				}
			}
		}

		Log.d(TAG, "image not found *__*");
		// image not found!
		return null;
	}

	/**
	 * In order to detect gestures, onDown must return true
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	/**
	 * A scroll has been performed
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.i(TAG, "SCROLL - x: " + distanceX + ", y: " + distanceY);

		// # implement me!
		/*
		 * if the distanceY is greater than 9 and distanceX in lower than 6,
		 * assume that a "thrown" gesture has been performed. Get the image (if
		 * any) using the first motion event (beware! might be null! use the
		 * second one in that case). If an image has been found, call
		 * activity.setDragged() and return true; return false otherwise.
		 */
		if (distanceY > 9 && distanceX < 6) {
			FileAwareImageView imageView = getImageCurrentlyAt(e1);
			if (imageView == null)
				imageView = getImageCurrentlyAt(e2);
			if (imageView == null)
				return false;
			else
				activity.setDragged(imageView);
			return true;
		} else {
			return false;
		}
		//
	}
}
