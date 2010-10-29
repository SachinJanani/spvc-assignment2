package dk.itu.spvc.dragdrop;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * A simple adapter for the gallery.
 * The images displayed are static, e.g. are in the drawable folder.
 * @author frza
 *
 */
public class LolImageAdapter extends BaseAdapter {
	int galleryItemBackground;
	Context context;
	
	//the array with the ids of the resources to be displayed
	private Integer[] ids = {
			R.drawable.lol1,
			R.drawable.lol2,
			R.drawable.lol3,
			R.drawable.lol4,
			R.drawable.lol5,
			R.drawable.lol6,
			R.drawable.lol7,
			R.drawable.lol8,
			R.drawable.lol9,
			R.drawable.lol10,
			R.drawable.lol11
	};
	
	public LolImageAdapter(Context c) {
		context = c;
		TypedArray a = c.obtainStyledAttributes(R.styleable.Gallery);
		galleryItemBackground = a.getResourceId(R.styleable.Gallery_android_galleryItemBackground, 0);
		a.recycle();
	}

	@Override
	public int getCount() {
		return ids.length;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Called when the gallery has to display an image.
	 * For simplicity, we return a new FileAwareImageView each time,
	 * but it could be possible to store Weakreferences to the previously
	 * created image and try to reuse them instead.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//create a new FileAwareImageView configured with this context and the ids[position] resource id.
		FileAwareImageView i = new FileAwareImageView(context, ids[position]);
		
		//create a 150x100 image
		i.setLayoutParams( new Gallery.LayoutParams(150,100) );
		//scale the image to FIT X and Y
		i.setScaleType(ImageView.ScaleType.FIT_XY);
		i.setBackgroundResource(galleryItemBackground);
		
		//return the image
		return i;
	}

}
