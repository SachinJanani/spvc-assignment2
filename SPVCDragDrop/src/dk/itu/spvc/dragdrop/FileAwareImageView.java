package dk.itu.spvc.dragdrop;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.widget.ImageView;

/**
 * A simple ImageView that "remember" the resource id that created the image.
 * Also returns an InputStream of the image itself.
 * @author frza
 *
 */
public class FileAwareImageView extends ImageView {

	int resourceId;
	AssetFileDescriptor fd;
	
	public FileAwareImageView(Context context, int resourceId) {
		super(context);
		this.resourceId = resourceId;
		//set the image
		this.setImageResource(resourceId);
		//open the file descriptor
		this.fd = this.getContext().getResources().openRawResourceFd(resourceId);
	}
	
	public InputStream getImageInputStream() throws IOException {
		return fd.createInputStream();
	}
	

}
