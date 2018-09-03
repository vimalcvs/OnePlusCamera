package camera.vimal.vk;

import android.app.Application;
import android.os.Process;
import android.util.Log;

public class OnePlusCameraApplication extends Application {
	private static final String TAG = "OnePlusApplication";

    @Override
    public void onCreate() {
		if( MyDebug.LOG )
			Log.d(TAG, "onCreate");
        super.onCreate();
        checkAppReplacingState();
    }

    private void checkAppReplacingState() {
		if( MyDebug.LOG )
			Log.d(TAG, "checkAppReplacingState");
        if( getResources() == null ) {
            Log.e(TAG, "app is replacing, kill");
            Process.killProcess(Process.myPid());
        }
    }
}
