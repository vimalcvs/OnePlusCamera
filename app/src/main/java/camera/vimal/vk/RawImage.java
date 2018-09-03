package camera.vimal.vk;

import android.annotation.TargetApi;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.os.Build;
import android.util.Log;


import java.io.IOException;
import java.io.OutputStream;


public class RawImage {
	private static final String TAG = "RawImage";

    private final DngCreator dngCreator;
    private final Image image;

    public RawImage(DngCreator dngCreator, Image image) {
        this.dngCreator = dngCreator;
        this.image = image;
    }

    /** Writes the dng file to the supplied output.
     */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void writeImage(OutputStream dngOutput) throws IOException {
		if( MyDebug.LOG )
			Log.d(TAG, "writeImage");
		try {
            dngCreator.writeImage(dngOutput, image);
        }
        catch(AssertionError e) {

		    e.printStackTrace();
		    throw new IOException();
        }
        catch(IllegalStateException e) {

		    e.printStackTrace();
		    throw new IOException();
        }
    }


	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void close() {
		if( MyDebug.LOG )
			Log.d(TAG, "close");
        image.close();
        dngCreator.close();
    }
}
