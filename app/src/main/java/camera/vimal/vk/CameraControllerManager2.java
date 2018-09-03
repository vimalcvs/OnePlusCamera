package camera.vimal.vk;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Log;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerManager2 extends CameraControllerManager {
	private static final String TAG = "CControllerManager2";

	private final Context context;

	public CameraControllerManager2(Context context) {
		this.context = context;
	}

	@Override
	public int getNumberOfCameras() {
		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try {
			return manager.getCameraIdList().length;
		}
		catch(Throwable e) {

			if( MyDebug.LOG )
				Log.e(TAG, "exception trying to get camera ids");
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean isFrontFacing(int cameraId) {
		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try {
			String cameraIdS = manager.getCameraIdList()[cameraId];
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
			return characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT;
		}
		catch(Throwable e) {

			if( MyDebug.LOG )
				Log.e(TAG, "exception trying to get camera characteristics");
			e.printStackTrace();
		}
		return false;
	}


	static boolean isHardwareLevelSupported(CameraCharacteristics c, int requiredLevel) {
		int deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
		if( MyDebug.LOG ) {
			switch (deviceLevel) {
				case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
					Log.d(TAG, "Camera has LEGACY Camera2 support");
					break;
				case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
					Log.d(TAG, "Camera has LIMITED Camera2 support");
					break;
				case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
					Log.d(TAG, "Camera has FULL Camera2 support");
					break;
				default:
					Log.d(TAG, "Camera has unknown Camera2 support: " + deviceLevel);
					break;
			}
		}
		if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
			return requiredLevel == deviceLevel;
		}

		return requiredLevel <= deviceLevel;
	}


	public boolean allowCamera2Support(int cameraId) {
		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try {
			String cameraIdS = manager.getCameraIdList()[cameraId];
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
			return isHardwareLevelSupported(characteristics, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
		}
		catch(Throwable e) {
				if( MyDebug.LOG )
				Log.e(TAG, "exception trying to get camera characteristics");
			e.printStackTrace();
		}
		return false;
	}
}
