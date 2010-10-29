package dk.itu.spvc.dragdrop;

import android.util.Log;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.FrameLayout;

/**
 * Gesture listener associated with a PlayAreaView. Responsible to drag the
 * image and get the "double tap" gesture to reset the application state
 * 
 * @author frza
 * 
 */
public class ImageDraggedDetector extends SimpleOnGestureListener {
	static final String DEBUG_TAG = "IMAGE_DRAGGED_DETECTOR";

	DragDropActivity activity;
	PlayAreaView view;
	int frame_w, frame_h;

	protected ImageDraggedDetector(DragDropActivity activity, PlayAreaView view) {
		this.activity = activity;
		FrameLayout frame = activity.frame;
		this.view = view;
		this.frame_w = frame.getWidth();
		this.frame_h = frame.getHeight();
	}

	/**
	 * Returns true if the x,y coordinates are inside the view
	 * 
	 * @param view
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean touch(float x, float y) {
		// luckily for us, the PlayAreaView implements an "isTouched" method.
		// Just return it
		return view.isTouched(x, y);
	}

	/**
	 * If the user performs a "doubleTab" gesture, call resetDragDrop on the
	 * activity and return true (we handled the gesture)
	 */
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		activity.resetDragDrop();
		return true;
	}

	/**
	 * In order to detec gestures, "onDown" must return true
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	/**
	 * Normalize return simply the cur/max value.
	 * 
	 * @param max
	 * @param cur
	 * @return
	 */
	float normalize(int max, float cur) {
		return cur / max;
	}

	/**
	 * The user performed a drag. Notice that the drag could have been performed
	 * everywhere, so we need to check that it has been performer on the dragged
	 * image, before moving it and sending the new coordinates to the server.
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.v(DEBUG_TAG, "onScroll");
		// check that the coordinates are inside the image
		if (touch(e2.getX(), e2.getY())) {
			// if so, call the onMove method on the view object (negate the
			// distanceX and Y values)
			view.onMove(-distanceX, -distanceY);
			// and call the "imageMoved" method on the activity, to send the new
			// coordinates.
			// Perform a normalization: send e2.getX()/frame_w and
			// e2.getY()/frame_h values

			// TODO: call the activity.imageMoved method with the appropriate
			// values

			// and return true, since we handled the gesture
			return true;
		}
		// mmhh, we didn't handled the gesture, return false as some other
		// gesture listener might want to catch this.
		return false;

	}
}
