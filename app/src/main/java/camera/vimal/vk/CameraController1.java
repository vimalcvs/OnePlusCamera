package camera.vimal.vk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;


public class CameraController1 extends CameraController {
	private static final String TAG = "CameraController1";

	private Camera camera;
    private int display_orientation;
    private final Camera.CameraInfo camera_info = new Camera.CameraInfo();
	private String iso_key;
	private boolean frontscreen_flash;
	private final ErrorCallback camera_error_cb;
	private boolean sounds_enabled = true;

	private int n_burst;
	private final List<byte []> pending_burst_images = new ArrayList<>();
	private List<Integer> burst_exposures;
	private boolean want_expo_bracketing;
	private final static int max_expo_bracketing_n_images = 3;
	private int expo_bracketing_n_images = 3;
	private double expo_bracketing_stops = 2.0;

	private int current_zoom_value;
	private int current_exposure_compensation;
	private int picture_width;
	private int picture_height;


	public CameraController1(int cameraId, final ErrorCallback camera_error_cb) throws CameraControllerException {
		super(cameraId);
		if( MyDebug.LOG )
			Log.d(TAG, "create new CameraController1: " + cameraId);
		this.camera_error_cb = camera_error_cb;
		try {
			camera = Camera.open(cameraId);
		}
		catch(RuntimeException e) {
			if( MyDebug.LOG )
				Log.e(TAG, "failed to open camera");
			e.printStackTrace();
			throw new CameraControllerException();
		}
		if( camera == null ) {
				if( MyDebug.LOG )
				Log.e(TAG, "camera.open returned null");
			throw new CameraControllerException();
		}
		try {
			Camera.getCameraInfo(cameraId, camera_info);
		}
		catch(RuntimeException e) {
				if( MyDebug.LOG )
				Log.e(TAG, "failed to get camera info");
			e.printStackTrace();
			this.release();
			throw new CameraControllerException();
		}


		final CameraErrorCallback camera_error_callback = new CameraErrorCallback();
		camera.setErrorCallback(camera_error_callback);


	}

	@Override
	public void onError() {
		Log.e(TAG, "onError");
		if( this.camera != null ) {
			this.camera.release();
			this.camera = null;
		}
		if( this.camera_error_cb != null ) {

			this.camera_error_cb.onError();
		}
	}
	
	private class CameraErrorCallback implements Camera.ErrorCallback {
		@Override
		public void onError(int error, Camera cam) {

			Log.e(TAG, "camera onError: " + error);
			if( error == Camera.CAMERA_ERROR_SERVER_DIED ) {
				Log.e(TAG, "    CAMERA_ERROR_SERVER_DIED");
				CameraController1.this.onError();
			}
			else if( error == Camera.CAMERA_ERROR_UNKNOWN  ) {
				Log.e(TAG, "    CAMERA_ERROR_UNKNOWN ");
			}
		}
	}
	
	public void release() {
		if( camera != null ) {
				camera.release();
			camera = null;
		}
	}

	private Camera.Parameters getParameters() {
		if( MyDebug.LOG )
			Log.d(TAG, "getParameters");
		return camera.getParameters();
	}
	
	private void setCameraParameters(Camera.Parameters parameters) {
		if( MyDebug.LOG )
			Log.d(TAG, "setCameraParameters");
	    try {
			camera.setParameters(parameters);
    		if( MyDebug.LOG )
    			Log.d(TAG, "done");
	    }
	    catch(RuntimeException e) {
	    	// just in case something has gone wrong
    		if( MyDebug.LOG )
    			Log.d(TAG, "failed to set parameters");
    		e.printStackTrace();
    		count_camera_parameters_exception++;
	    }
	}
	
	private List<String> convertFlashModesToValues(List<String> supported_flash_modes) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "convertFlashModesToValues()");
			Log.d(TAG, "supported_flash_modes: " + supported_flash_modes);
		}
		List<String> output_modes = new ArrayList<>();
		if( supported_flash_modes != null ) {
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_OFF) ) {
				output_modes.add("flash_off");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_off");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_AUTO) ) {
				output_modes.add("flash_auto");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_auto");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_ON) ) {
				output_modes.add("flash_on");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_on");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_TORCH) ) {
				output_modes.add("flash_torch");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_torch");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_RED_EYE) ) {
				output_modes.add("flash_red_eye");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_red_eye");
			}
		}
		
				if( output_modes.size() > 1 ) {
			if( MyDebug.LOG )
				Log.d(TAG, "flash supported");
		}
		else {
			if( isFrontFacing() ) {
				if( MyDebug.LOG )
					Log.d(TAG, "front-screen with no flash");
				output_modes.clear();
				output_modes.add("flash_off");
				output_modes.add("flash_frontscreen_on");
				output_modes.add("flash_frontscreen_torch");
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "no flash");

				output_modes.clear();
			}
		}

		return output_modes;
	}

	private List<String> convertFocusModesToValues(List<String> supported_focus_modes) {
		if( MyDebug.LOG )
			Log.d(TAG, "convertFocusModesToValues()");
		List<String> output_modes = new ArrayList<>();
		if( supported_focus_modes != null ) {

			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ) {
				output_modes.add("focus_mode_auto");
				if( MyDebug.LOG ) {
					Log.d(TAG, " supports focus_mode_auto");
				}
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) ) {
				output_modes.add("focus_mode_infinity");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_infinity");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_MACRO) ) {
				output_modes.add("focus_mode_macro");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_macro");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ) {
				output_modes.add("focus_mode_locked");
				if( MyDebug.LOG ) {
					Log.d(TAG, " supports focus_mode_locked");
				}
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_FIXED) ) {
				output_modes.add("focus_mode_fixed");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_fixed");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_EDOF) ) {
				output_modes.add("focus_mode_edof");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_edof");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ) {
				output_modes.add("focus_mode_continuous_picture");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_continuous_picture");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) {
				output_modes.add("focus_mode_continuous_video");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_continuous_video");
			}
		}
		return output_modes;
	}
	
	public String getAPI() {
		return "Camera";
	}
	
	public CameraFeatures getCameraFeatures() throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "getCameraFeatures()");
	    Camera.Parameters parameters = this.getParameters();
	    CameraFeatures camera_features = new CameraFeatures();
		camera_features.is_zoom_supported = parameters.isZoomSupported();
		if( camera_features.is_zoom_supported ) {
			camera_features.max_zoom = parameters.getMaxZoom();
			try {
				camera_features.zoom_ratios = parameters.getZoomRatios();
			}
			catch(NumberFormatException e) {
        			if( MyDebug.LOG )
	    			Log.e(TAG, "NumberFormatException in getZoomRatios()");
				e.printStackTrace();
				camera_features.is_zoom_supported = false;
				camera_features.max_zoom = 0;
				camera_features.zoom_ratios = null;
			}
		}

		camera_features.supports_face_detection = parameters.getMaxNumDetectedFaces() > 0;


		List<Camera.Size> camera_picture_sizes = parameters.getSupportedPictureSizes();
		if( camera_picture_sizes == null ) {
			Log.e(TAG, "getSupportedPictureSizes() returned null!");
			throw new CameraControllerException();
		}
		camera_features.picture_sizes = new ArrayList<>();
			for(Camera.Size camera_size : camera_picture_sizes) {
			camera_features.picture_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

          List<String> supported_flash_modes = parameters.getSupportedFlashModes();
		camera_features.supported_flash_values = convertFlashModesToValues(supported_flash_modes);

        List<String> supported_focus_modes = parameters.getSupportedFocusModes();
		camera_features.supported_focus_values = convertFocusModesToValues(supported_focus_modes);
		camera_features.max_num_focus_areas = parameters.getMaxNumFocusAreas();

        camera_features.is_exposure_lock_supported = parameters.isAutoExposureLockSupported();

        camera_features.is_video_stabilization_supported = parameters.isVideoStabilizationSupported();

		camera_features.is_photo_video_recording_supported = parameters.isVideoSnapshotSupported();
        
        camera_features.min_exposure = parameters.getMinExposureCompensation();
        camera_features.max_exposure = parameters.getMaxExposureCompensation();
		camera_features.exposure_step = getExposureCompensationStep();
		camera_features.supports_expo_bracketing = ( camera_features.min_exposure != 0 && camera_features.max_exposure != 0 );
		camera_features.max_expo_bracketing_n_images = max_expo_bracketing_n_images;

		List<Camera.Size> camera_video_sizes = parameters.getSupportedVideoSizes();
    	if( camera_video_sizes == null ) {
    			if( MyDebug.LOG )
    			Log.d(TAG, "take video_sizes from preview sizes");
    		camera_video_sizes = parameters.getSupportedPreviewSizes();
    	}
		camera_features.video_sizes = new ArrayList<>();
		for(Camera.Size camera_size : camera_video_sizes) {
			camera_features.video_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

		List<Camera.Size> camera_preview_sizes = parameters.getSupportedPreviewSizes();
		camera_features.preview_sizes = new ArrayList<>();
		for(Camera.Size camera_size : camera_preview_sizes) {
			camera_features.preview_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

		if( MyDebug.LOG )
			Log.d(TAG, "camera parameters: " + parameters.flatten());

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {

        	camera_features.can_disable_shutter_sound = camera_info.canDisableShutterSound;
        }
        else {
        	camera_features.can_disable_shutter_sound = false;
        }

		final float default_view_angle_x = 55.0f;
		final float default_view_angle_y = 43.0f;
		try {
			camera_features.view_angle_x = parameters.getHorizontalViewAngle();
			camera_features.view_angle_y = parameters.getVerticalViewAngle();
		}
		catch(Exception e) {

			e.printStackTrace();
			Log.e(TAG, "exception reading horizontal or vertical view angles");
			camera_features.view_angle_x = default_view_angle_x;
			camera_features.view_angle_y = default_view_angle_y;
		}
		if( MyDebug.LOG ) {
			Log.d(TAG, "view_angle_x: " + camera_features.view_angle_x);
			Log.d(TAG, "view_angle_y: " + camera_features.view_angle_y);
		}
		// need to handle some devices reporting rubbish
		if( camera_features.view_angle_x > 150.0f || camera_features.view_angle_y > 150.0f ) {
			Log.e(TAG, "camera API reporting stupid view angles, set to sensible defaults");
			camera_features.view_angle_x = default_view_angle_x;
			camera_features.view_angle_y = default_view_angle_y;
		}

		return camera_features;
	}
	

	@Override
	public SupportedValues setSceneMode(String value) {
		Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedSceneModes();

		SupportedValues supported_values = checkModeIsSupported(values, value, SCENE_MODE_DEFAULT);
		if( supported_values != null ) {
			String scene_mode = parameters.getSceneMode();

			if( scene_mode != null && !scene_mode.equals(supported_values.selected_value) ) {
	        	parameters.setSceneMode(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}
	
	@Override
	public String getSceneMode() {
    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getSceneMode();
	}

	@Override
	public boolean sceneModeAffectsFunctionality() {

		return true;
	}

	public SupportedValues setColorEffect(String value) {
		Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedColorEffects();
		SupportedValues supported_values = checkModeIsSupported(values, value, COLOR_EFFECT_DEFAULT);
		if( supported_values != null ) {
			String color_effect = parameters.getColorEffect();

			if( color_effect == null || !color_effect.equals(supported_values.selected_value) ) {
	        	parameters.setColorEffect(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}

	public String getColorEffect() {
    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getColorEffect();
	}

	public SupportedValues setWhiteBalance(String value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setWhiteBalance: " + value);
		Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedWhiteBalance();
		if( values != null ) {

			while( values.contains("manual") ) {
				values.remove("manual");
			}
		}
		SupportedValues supported_values = checkModeIsSupported(values, value, WHITE_BALANCE_DEFAULT);
		if( supported_values != null ) {
			String white_balance = parameters.getWhiteBalance();

			if( white_balance != null && !white_balance.equals(supported_values.selected_value) ) {
	        	parameters.setWhiteBalance(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}

	public String getWhiteBalance() {
    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getWhiteBalance();
	}

	@Override
	public boolean setWhiteBalanceTemperature(int temperature) {

		return false;
	}

	@Override
	public int getWhiteBalanceTemperature() {

		return 0;
	}

	@Override
	public SupportedValues setAntiBanding(String value) {
		Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedAntibanding();
		SupportedValues supported_values = checkModeIsSupported(values, value, ANTIBANDING_DEFAULT);
		if( supported_values != null ) {

			if( supported_values.selected_value.equals(value) ) {
				String antibanding = parameters.getAntibanding();
				if( antibanding == null || !antibanding.equals(supported_values.selected_value) ) {
					parameters.setAntibanding(supported_values.selected_value);
					setCameraParameters(parameters);
				}
			}
		}
		return supported_values;
	}

	@Override
	public String getAntiBanding() {
    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getAntibanding();
	}

	@Override
	public SupportedValues setISO(String value) {
    	Camera.Parameters parameters = this.getParameters();
		String iso_values = parameters.get("iso-values");
		if( iso_values == null ) {
			iso_values = parameters.get("iso-mode-values");
			if( iso_values == null ) {
				iso_values = parameters.get("iso-speed-values");
				if( iso_values == null )
					iso_values = parameters.get("nv-picture-iso-values");
			}
		}
		List<String> values = null;
		if( iso_values != null && iso_values.length() > 0 ) {
			if( MyDebug.LOG )
				Log.d(TAG, "iso_values: " + iso_values);
			String [] isos_array = iso_values.split(",");

			if( isos_array.length > 0 ) {

				HashSet<String> hashSet = new HashSet<>();
				values = new ArrayList<>();
					for(String iso : isos_array) {
					if( !hashSet.contains(iso) ) {
						values.add(iso);
						hashSet.add(iso);
					}
				}
			}
		}

		iso_key = "iso";
		if( parameters.get(iso_key) == null ) {
			iso_key = "iso-speed";
			if( parameters.get(iso_key) == null ) {
				iso_key = "nv-picture-iso";
				if( parameters.get(iso_key) == null ) {
					if ( Build.MODEL.contains("Z00") )
						iso_key = "iso";
					else
						iso_key = null;
				}
			}
		}

		if( iso_key != null ){
			if( values == null ) {

				values = new ArrayList<>();
				values.add(ISO_DEFAULT);
				values.add("50");
				values.add("100");
				values.add("200");
				values.add("400");
				values.add("800");
				values.add("1600");
			}
			SupportedValues supported_values = checkModeIsSupported(values, value, ISO_DEFAULT);
			if( supported_values != null ) {
				if( MyDebug.LOG )
					Log.d(TAG, "set: " + iso_key + " to: " + supported_values.selected_value);
	        	parameters.set(iso_key, supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
			return supported_values;
		}
		return null;
	}

	@Override
	public String getISOKey() {
		if( MyDebug.LOG )
			Log.d(TAG, "getISOKey");
    	return this.iso_key;
    }

	@Override
	public void setManualISO(boolean manual_iso, int iso) {

	}

	@Override
	public boolean isManualISO() {

		return false;
	}

	@Override
	public boolean setISO(int iso) {

		return false;
	}

	@Override
	public int getISO() {

		return 0;
	}

	@Override
	public long getExposureTime() {

		return 0L;
	}

	@Override
	public boolean setExposureTime(long exposure_time) {

		return false;
	}
	
	@Override
    public CameraController.Size getPictureSize() {
    	return new CameraController.Size(picture_width, picture_height);
    }

	@Override
	public void setPictureSize(int width, int height) {
    	Camera.Parameters parameters = this.getParameters();
		this.picture_width = width;
		this.picture_height = height;
		parameters.setPictureSize(width, height);
		if( MyDebug.LOG )
			Log.d(TAG, "set picture size: " + parameters.getPictureSize().width + ", " + parameters.getPictureSize().height);
    	setCameraParameters(parameters);
	}
    
	@Override
    public CameraController.Size getPreviewSize() {
    	Camera.Parameters parameters = this.getParameters();
    	Camera.Size camera_size = parameters.getPreviewSize();
    	return new CameraController.Size(camera_size.width, camera_size.height);
    }

	@Override
	public void setPreviewSize(int width, int height) {
    	Camera.Parameters parameters = this.getParameters();
		if( MyDebug.LOG )
			Log.d(TAG, "current preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
        parameters.setPreviewSize(width, height);
		if( MyDebug.LOG )
			Log.d(TAG, "new preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
    	setCameraParameters(parameters);
    }
	
	@Override
	public void setWantBurst(boolean want_burst) {

	}

	@Override
	public void setBurstNImages(int burst_requested_n_images) {

	}

	@Override
	public void setBurstForNoiseReduction(boolean burst_for_noise_reduction) {

	}

	@Override
	public void setExpoBracketing(boolean want_expo_bracketing) {
		if( MyDebug.LOG )
			Log.d(TAG, "setExpoBracketing: " + want_expo_bracketing);
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.e(TAG, "no camera");
			return;
		}
		if( this.want_expo_bracketing == want_expo_bracketing ) {
			return;
		}
		this.want_expo_bracketing = want_expo_bracketing;
	}

	@Override
	public void setExpoBracketingNImages(int n_images) {
		if( MyDebug.LOG )
			Log.d(TAG, "setExpoBracketingNImages: " + n_images);
		if( n_images <= 1 || (n_images % 2) == 0 ) {
			if( MyDebug.LOG )
				Log.e(TAG, "n_images should be an odd number greater than 1");
			throw new RuntimeException();
		}
		if( n_images > max_expo_bracketing_n_images ) {
			n_images = max_expo_bracketing_n_images;
			if( MyDebug.LOG )
				Log.e(TAG, "limiting n_images to max of " + n_images);
		}
		this.expo_bracketing_n_images = n_images;
	}

	@Override
	public void setExpoBracketingStops(double stops) {
		if( MyDebug.LOG )
			Log.d(TAG, "setExpoBracketingStops: " + stops);
		if( stops <= 0.0 ) {
			if( MyDebug.LOG )
				Log.e(TAG, "stops should be positive");
			throw new RuntimeException();
		}
		this.expo_bracketing_stops = stops;
	}

	@Override
	public void setUseExpoFastBurst(boolean use_expo_fast_burst) {

	}

	@Override
	public void setOptimiseAEForDRO(boolean optimise_ae_for_dro) {

	}

	@Override
	public void setRaw(boolean want_raw, int max_raw_images) {

	}

	@Override
	public void setVideoHighSpeed(boolean setVideoHighSpeed) {

	}

	@Override
	public void setVideoStabilization(boolean enabled) {
	    Camera.Parameters parameters = this.getParameters();
        parameters.setVideoStabilization(enabled);
    	setCameraParameters(parameters);
	}
	
	public boolean getVideoStabilization() {
	    Camera.Parameters parameters = this.getParameters();
        return parameters.getVideoStabilization();
	}

	@Override
	public void setLogProfile(boolean use_log_profile, float log_profile_strength) {

	}

	@Override
	public boolean isLogProfile() {

		return false;
	}

	public int getJpegQuality() {
	    Camera.Parameters parameters = this.getParameters();
	    return parameters.getJpegQuality();
	}
	
	public void setJpegQuality(int quality) {
	    Camera.Parameters parameters = this.getParameters();
		parameters.setJpegQuality(quality);
    	setCameraParameters(parameters);
	}
	
	public int getZoom() {

		return this.current_zoom_value;
	}
	
	public void setZoom(int value) {
		Camera.Parameters parameters = this.getParameters();
		if( MyDebug.LOG )
			Log.d(TAG, "zoom was: " + parameters.getZoom());
		this.current_zoom_value = value;
		parameters.setZoom(value);
    	setCameraParameters(parameters);
	}

	public int getExposureCompensation() {

		return this.current_exposure_compensation;
	}

	private float getExposureCompensationStep() {
		float exposure_step;
		Camera.Parameters parameters = this.getParameters();
        try {
        	exposure_step = parameters.getExposureCompensationStep();
        }
        catch(Exception e) {
        		if( MyDebug.LOG )
    			Log.e(TAG, "exception from getExposureCompensationStep()");
        	e.printStackTrace();
        	exposure_step = 1.0f/3.0f;
        }
        return exposure_step;
	}
	

	public boolean setExposureCompensation(int new_exposure) {

		if( new_exposure != current_exposure_compensation ) {
			if( MyDebug.LOG )
				Log.d(TAG, "change exposure from " + current_exposure_compensation + " to " + new_exposure);
			Camera.Parameters parameters = this.getParameters();
			this.current_exposure_compensation = new_exposure;
			parameters.setExposureCompensation(new_exposure);
        	setCameraParameters(parameters);
        	return true;
		}
		return false;
	}
	
	public void setPreviewFpsRange(int min, int max) {
    	if( MyDebug.LOG )
    		Log.d(TAG, "setPreviewFpsRange: " + min + " to " + max);
		try {
			Camera.Parameters parameters = this.getParameters();
			parameters.setPreviewFpsRange(min, max);
			setCameraParameters(parameters);
		}
		catch(RuntimeException e) {

    		Log.e(TAG, "setPreviewFpsRange failed to get parameters");
			e.printStackTrace();
		}
	}
	
	public List<int []> getSupportedPreviewFpsRange() {
		Camera.Parameters parameters = this.getParameters();
		try {
			return parameters.getSupportedPreviewFpsRange();
		}
		catch(StringIndexOutOfBoundsException e) {

			e.printStackTrace();
	    	if( MyDebug.LOG ) {
	    		Log.e(TAG, "getSupportedPreviewFpsRange() gave StringIndexOutOfBoundsException");
	    	}
		}
		return null;
	}
	
	@Override
	public void setFocusValue(String focus_value) {
		Camera.Parameters parameters = this.getParameters();
		switch(focus_value) {
			case "focus_mode_auto":
			case "focus_mode_locked":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				break;
			case "focus_mode_infinity":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
				break;
			case "focus_mode_macro":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
				break;
			case "focus_mode_fixed":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
				break;
			case "focus_mode_edof":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
				break;
			case "focus_mode_continuous_picture":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				break;
			case "focus_mode_continuous_video":
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				break;
			default:
				if (MyDebug.LOG)
					Log.d(TAG, "setFocusValue() received unknown focus value " + focus_value);
				break;
		}
    	setCameraParameters(parameters);
	}
	
	private String convertFocusModeToValue(String focus_mode) {

		if( MyDebug.LOG )
			Log.d(TAG, "convertFocusModeToValue: " + focus_mode);
		String focus_value = "";
		if( focus_mode == null ) {

		}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ) {
    		focus_value = "focus_mode_auto";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_INFINITY) ) {
    		focus_value = "focus_mode_infinity";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) {
    		focus_value = "focus_mode_macro";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_FIXED) ) {
    		focus_value = "focus_mode_fixed";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_EDOF) ) {
    		focus_value = "focus_mode_edof";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ) {
    		focus_value = "focus_mode_continuous_picture";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) {
    		focus_value = "focus_mode_continuous_video";
    	}
    	return focus_value;
	}
	
	@Override
	public String getFocusValue() {

		Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
				return convertFocusModeToValue(focus_mode);
	}

	@Override
	public float getFocusDistance() {

		return 0.0f;
	}

	@Override
	public boolean setFocusDistance(float focus_distance) {

		return false;
	}

	private String convertFlashValueToMode(String flash_value) {
		String flash_mode = "";
		switch(flash_value) {
			case "flash_off":
				flash_mode = Camera.Parameters.FLASH_MODE_OFF;
				break;
			case "flash_auto":
				flash_mode = Camera.Parameters.FLASH_MODE_AUTO;
				break;
			case "flash_on":
				flash_mode = Camera.Parameters.FLASH_MODE_ON;
				break;
			case "flash_torch":
				flash_mode = Camera.Parameters.FLASH_MODE_TORCH;
				break;
			case "flash_red_eye":
				flash_mode = Camera.Parameters.FLASH_MODE_RED_EYE;
				break;
			case "flash_frontscreen_on":
			case "flash_frontscreen_torch":
				flash_mode = Camera.Parameters.FLASH_MODE_OFF;
				break;
		}
    	return flash_mode;
	}
	
	public void setFlashValue(String flash_value) {
		Camera.Parameters parameters = this.getParameters();
		if( MyDebug.LOG )
			Log.d(TAG, "setFlashValue: " + flash_value);

		this.frontscreen_flash = false;
    	if( flash_value.equals("flash_frontscreen_on") ) {
    			this.frontscreen_flash = true;
    		return;
    	}
		
    	if( parameters.getFlashMode() == null ) {
    		if( MyDebug.LOG )
    			Log.d(TAG, "flash mode not supported");
			return;
    	}

		final String flash_mode = convertFlashValueToMode(flash_value);
    	if( flash_mode.length() > 0 && !flash_mode.equals(parameters.getFlashMode()) ) {
    		if( parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH) && !flash_mode.equals(Camera.Parameters.FLASH_MODE_OFF) ) {
    				if( MyDebug.LOG )
    				Log.d(TAG, "first turn torch off");
        		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            	setCameraParameters(parameters);

            	Handler handler = new Handler();
            	handler.postDelayed(new Runnable(){
            		@Override
            	    public void run(){
            			if( MyDebug.LOG )
            				Log.d(TAG, "now set actual flash mode after turning torch off");
            			if( camera != null ) {
	            			Camera.Parameters parameters = getParameters();
	                		parameters.setFlashMode(flash_mode);
	                    	setCameraParameters(parameters);
            			}
            	   }
            	}, 100);
    		}
    		else {
        		parameters.setFlashMode(flash_mode);
            	setCameraParameters(parameters);
    		}
    	}
	}
	
	private String convertFlashModeToValue(String flash_mode) {
			if( MyDebug.LOG )
			Log.d(TAG, "convertFlashModeToValue: " + flash_mode);
		String flash_value = "";
		if( flash_mode == null ) {
			}
		else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_OFF) ) {
    		flash_value = "flash_off";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_AUTO) ) {
    		flash_value = "flash_auto";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_ON) ) {
    		flash_value = "flash_on";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_TORCH) ) {
    		flash_value = "flash_torch";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_RED_EYE) ) {
    		flash_value = "flash_red_eye";
    	}
    	return flash_value;
	}
	
	public String getFlashValue() {

		Camera.Parameters parameters = this.getParameters();
		String flash_mode = parameters.getFlashMode();
		return convertFlashModeToValue(flash_mode);
	}
	
	public void setRecordingHint(boolean hint) {
		if( MyDebug.LOG )
			Log.d(TAG, "setRecordingHint: " + hint);
		try {
			Camera.Parameters parameters = this.getParameters();
					String focus_mode = parameters.getFocusMode();
					if( focus_mode != null && !focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) {
				parameters.setRecordingHint(hint);
				setCameraParameters(parameters);
			}
		}
		catch(RuntimeException e) {
				Log.e(TAG, "setRecordingHint failed to get parameters");
			e.printStackTrace();
		}
	}

	public void setAutoExposureLock(boolean enabled) {
		Camera.Parameters parameters = this.getParameters();
		parameters.setAutoExposureLock(enabled);
    	setCameraParameters(parameters);
	}
	
	public boolean getAutoExposureLock() {
		Camera.Parameters parameters = this.getParameters();
		if( !parameters.isAutoExposureLockSupported() )
			return false;
		return parameters.getAutoExposureLock();
	}

	public void setRotation(int rotation) {
		Camera.Parameters parameters = this.getParameters();
		parameters.setRotation(rotation);
    	setCameraParameters(parameters);
	}
	
	public void setLocationInfo(Location location) {
        Camera.Parameters parameters = this.getParameters();
        parameters.removeGpsData();
        parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);
        parameters.setGpsLatitude(location.getLatitude());
        parameters.setGpsLongitude(location.getLongitude());
        parameters.setGpsProcessingMethod(location.getProvider());
        if( location.hasAltitude() ) {
            parameters.setGpsAltitude(location.getAltitude());
        }
        else {

            parameters.setGpsAltitude(0);
        }
        if( location.getTime() != 0 ) {
        	parameters.setGpsTimestamp(location.getTime() / 1000);
        }
    	setCameraParameters(parameters);
	}
	
	public void removeLocationInfo() {
        Camera.Parameters parameters = this.getParameters();
        parameters.removeGpsData();
    	setCameraParameters(parameters);
	}

	public void enableShutterSound(boolean enabled) {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
        	camera.enableShutterSound(enabled);
        }
		sounds_enabled = enabled;
	}
	
	public boolean setFocusAndMeteringArea(List<CameraController.Area> areas) {
		List<Camera.Area> camera_areas = new ArrayList<>();
		for(CameraController.Area area : areas) {
			camera_areas.add(new Camera.Area(area.rect, area.weight));
		}
        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		   if( parameters.getMaxNumFocusAreas() != 0 && focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) ) {
		    parameters.setFocusAreas(camera_areas);

		     if( parameters.getMaxNumMeteringAreas() == 0 ) {
        		if( MyDebug.LOG )
        			Log.d(TAG, "metering areas not supported");
		    }
		    else {
		    	parameters.setMeteringAreas(camera_areas);
		    }

		    setCameraParameters(parameters);

		    return true;
        }
        else if( parameters.getMaxNumMeteringAreas() != 0 ) {
	    	parameters.setMeteringAreas(camera_areas);

		    setCameraParameters(parameters);
        }
        return false;
	}
	
	public void clearFocusAndMetering() {
        Camera.Parameters parameters = this.getParameters();
        boolean update_parameters = false;
        if( parameters.getMaxNumFocusAreas() > 0 ) {
        	parameters.setFocusAreas(null);
        	update_parameters = true;
        }
        if( parameters.getMaxNumMeteringAreas() > 0 ) {
        	parameters.setMeteringAreas(null);
        	update_parameters = true;
        }
        if( update_parameters ) {
		    setCameraParameters(parameters);
        }
	}
	
	public List<CameraController.Area> getFocusAreas() {
        Camera.Parameters parameters = this.getParameters();
		List<Camera.Area> camera_areas = parameters.getFocusAreas();
		if( camera_areas == null )
			return null;
		List<CameraController.Area> areas = new ArrayList<>();
		for(Camera.Area camera_area : camera_areas) {
			areas.add(new CameraController.Area(camera_area.rect, camera_area.weight));
		}
		return areas;
	}

	public List<CameraController.Area> getMeteringAreas() {
        Camera.Parameters parameters = this.getParameters();
		List<Camera.Area> camera_areas = parameters.getMeteringAreas();
		if( camera_areas == null )
			return null;
		List<CameraController.Area> areas = new ArrayList<>();
		for(Camera.Area camera_area : camera_areas) {
			areas.add(new CameraController.Area(camera_area.rect, camera_area.weight));
		}
		return areas;
	}

	@Override
	public boolean supportsAutoFocus() {
        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
	   if( focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) ) {
        	return true;
        }
        return false;
	}
	
	@Override
	public boolean focusIsContinuous() {
        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		   if( focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) ) {
        	return true;
        }
        return false;
	}
	
	public boolean focusIsVideo() {
		Camera.Parameters parameters = this.getParameters();
		String current_focus_mode = parameters.getFocusMode();

		boolean focus_is_video = current_focus_mode != null && current_focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		if( MyDebug.LOG ) {
			Log.d(TAG, "current_focus_mode: " + current_focus_mode);
			Log.d(TAG, "focus_is_video: " + focus_is_video);
		}
		return focus_is_video;
	}
	
	@Override
	public 
	void reconnect() throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "reconnect");
		try {
			camera.reconnect();
		}
		catch(IOException e) {
			if( MyDebug.LOG )
				Log.e(TAG, "reconnect threw IOException");
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}
	
	@Override
	public void setPreviewDisplay(SurfaceHolder holder) throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewDisplay");
		try {
			camera.setPreviewDisplay(holder);
		}
		catch(IOException e) {
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}

	@Override
	public void setPreviewTexture(SurfaceTexture texture) throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewTexture");
		try {
			camera.setPreviewTexture(texture);
		}
		catch(IOException e) {
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}

	@Override
	public void startPreview() throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "startPreview");
		try {
			camera.startPreview();
		}
		catch(RuntimeException e) {
			if( MyDebug.LOG )
				Log.e(TAG, "failed to start preview");
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}
	
	@Override
	public void stopPreview() {
		if( camera != null ) {

			camera.stopPreview();
		}
	}


	public boolean startFaceDetection() {
	    try {
			camera.startFaceDetection();
	    }
	    catch(RuntimeException e) {
			if( MyDebug.LOG )
				Log.d(TAG, "face detection failed or already started");
	    	return false;
	    }
	    return true;
	}
	
	public void setFaceDetectionListener(final CameraController.FaceDetectionListener listener) {
		class CameraFaceDetectionListener implements Camera.FaceDetectionListener {
		    @Override
		    public void onFaceDetection(Camera.Face[] camera_faces, Camera camera) {
		    	Face [] faces = new Face[camera_faces.length];
		    	for(int i=0;i<camera_faces.length;i++) {
		    		faces[i] = new Face(camera_faces[i].score, camera_faces[i].rect);
		    	}
		    	listener.onFaceDetection(faces);
		    }
		}
		camera.setFaceDetectionListener(new CameraFaceDetectionListener());
	}

	@Override
	public void autoFocus(final CameraController.AutoFocusCallback cb, boolean capture_follows_autofocus_hint) {
		if( MyDebug.LOG )
			Log.d(TAG, "autoFocus");
        Camera.AutoFocusCallback camera_cb = new Camera.AutoFocusCallback() {
    		boolean done_autofocus = false;

    		@Override
			public void onAutoFocus(boolean success, Camera camera) {
				if( MyDebug.LOG )
					Log.d(TAG, "autoFocus.onAutoFocus");
					if( !done_autofocus ) {
					done_autofocus = true;
					cb.onAutoFocus(success);
				}
				else {
					if( MyDebug.LOG )
						Log.e(TAG, "ignore repeated autofocus");
				}
			}
        };
        try {
        	camera.autoFocus(camera_cb);
        }
		catch(RuntimeException e) {
			if( MyDebug.LOG )
				Log.e(TAG, "runtime exception from autoFocus");
			e.printStackTrace();
					cb.onAutoFocus(false);
		}
	}

	@Override
	public void setCaptureFollowAutofocusHint(boolean capture_follows_autofocus_hint) {

	}

	@Override
	public void cancelAutoFocus() {
		try {
			camera.cancelAutoFocus();
		}
		catch(RuntimeException e) {
				if( MyDebug.LOG )
				Log.d(TAG, "cancelAutoFocus() failed");
    		e.printStackTrace();
		}
	}
	
	@Override
	public void setContinuousFocusMoveCallback(final ContinuousFocusMoveCallback cb) {
		if( MyDebug.LOG )
			Log.d(TAG, "setContinuousFocusMoveCallback");
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {

			try {
				if( cb != null ) {
					camera.setAutoFocusMoveCallback(new AutoFocusMoveCallback() {
						@Override
						public void onAutoFocusMoving(boolean start, Camera camera) {
							if( MyDebug.LOG )
								Log.d(TAG, "onAutoFocusMoving: " + start);
							cb.onContinuousFocusMove(start);
						}
					});
				}
				else {
					camera.setAutoFocusMoveCallback(null);
				}
			}
			catch(RuntimeException e) {
						if( MyDebug.LOG )
					Log.e(TAG, "runtime exception from setAutoFocusMoveCallback");
				e.printStackTrace();
			}
		}
		else {
			if( MyDebug.LOG )
				Log.d(TAG, "setContinuousFocusMoveCallback requires Android JELLY_BEAN or higher");
		}
	}

	private static class TakePictureShutterCallback implements Camera.ShutterCallback {
			@Override
        public void onShutter() {
			if( MyDebug.LOG )
				Log.d(TAG, "shutterCallback.onShutter()");
        }
	}
	
	private void clearPending() {
		if( MyDebug.LOG )
			Log.d(TAG, "clearPending");
		pending_burst_images.clear();
		burst_exposures = null;
		n_burst = 0;
	}

	private void takePictureNow(final CameraController.PictureCallback picture, final ErrorCallback error) {
		if( MyDebug.LOG )
			Log.d(TAG, "takePictureNow");


    	final Camera.ShutterCallback shutter = sounds_enabled ? new TakePictureShutterCallback() : null;
        final Camera.PictureCallback camera_jpeg = picture == null ? null : new Camera.PictureCallback() {
    	    public void onPictureTaken(byte[] data, Camera cam) {
				if( MyDebug.LOG )
					Log.d(TAG, "onPictureTaken");
    	    		if( want_expo_bracketing && n_burst > 1 ) {
					pending_burst_images.add(data);
					if( pending_burst_images.size() >= n_burst ) {
						if( MyDebug.LOG )
							Log.d(TAG, "all burst images available");
						if( pending_burst_images.size() > n_burst ) {
							Log.e(TAG, "pending_burst_images size " + pending_burst_images.size() + " is greater than n_burst " + n_burst);
						}


						setExposureCompensation(burst_exposures.get(0));

							int n_half_images = pending_burst_images.size()/2;
						List<byte []> images = new ArrayList<>();

						for(int i=0;i<n_half_images;i++) {
							images.add(pending_burst_images.get(i+1));
						}

						images.add(pending_burst_images.get(0));

						for(int i=0;i<n_half_images;i++) {
							images.add(pending_burst_images.get(n_half_images+1));
						}

						picture.onBurstPictureTaken(images);
						pending_burst_images.clear();
						picture.onCompleted();
					}
					else {
						if( MyDebug.LOG )
							Log.d(TAG, "number of burst images is now: " + pending_burst_images.size());

						setExposureCompensation(burst_exposures.get(pending_burst_images.size()));
		try {
							startPreview();
						}
						catch(CameraControllerException e) {
							if( MyDebug.LOG )
								Log.d(TAG, "CameraControllerException trying to startPreview");
							e.printStackTrace();
						}

						Handler handler = new Handler();
						handler.postDelayed(new Runnable(){
							@Override
							public void run(){
								if( MyDebug.LOG )
									Log.d(TAG, "take picture after delay for next expo");
								if( camera != null ) {
									takePictureNow(picture, error);
								}
						   }
						}, 1000);
					}
				}
				else {
					picture.onPictureTaken(data);
					picture.onCompleted();
				}
    	    }
        };

		if( picture != null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "call onStarted() in callback");
			picture.onStarted();
		}
        try {
        	camera.takePicture(shutter, null, camera_jpeg);
        }
		catch(RuntimeException e) {
				if( MyDebug.LOG )
				Log.e(TAG, "runtime exception from takePicture");
			e.printStackTrace();
			error.onError();
		}
	}

	public void takePicture(final CameraController.PictureCallback picture, final ErrorCallback error) {
		if( MyDebug.LOG )
			Log.d(TAG, "takePicture");

		clearPending();
        if( want_expo_bracketing ) {
			if( MyDebug.LOG )
				Log.d(TAG, "set up expo bracketing");
			Camera.Parameters parameters = this.getParameters();
			int n_half_images = expo_bracketing_n_images/2;
			int min_exposure = parameters.getMinExposureCompensation();
			int max_exposure = parameters.getMaxExposureCompensation();
			float exposure_step = getExposureCompensationStep();
			if( exposure_step == 0.0f )
	        	exposure_step = 1.0f/3.0f;
			int exposure_current = getExposureCompensation();
			double stops_per_image = expo_bracketing_stops / (double)n_half_images;
			int steps = (int)((stops_per_image+1.0e-5) / exposure_step);
			steps = Math.max(steps, 1);
			if( MyDebug.LOG ) {
				Log.d(TAG, "steps: " + steps);
				Log.d(TAG, "exposure_current: " + exposure_current);
			}

			List<Integer> requests = new ArrayList<>();

				requests.add(exposure_current);


			for(int i=0;i<n_half_images;i++) {
				int exposure = exposure_current - (n_half_images-i)*steps;
				exposure = Math.max(exposure, min_exposure);
				requests.add(exposure);
				if( MyDebug.LOG ) {
					Log.d(TAG, "add burst request for " + i + "th dark image:");
					Log.d(TAG, "exposure: " + exposure);
				}
			}


			for(int i=0;i<n_half_images;i++) {
				int exposure = exposure_current + (i+1)*steps;
				exposure = Math.min(exposure, max_exposure);
				requests.add(exposure);
				if( MyDebug.LOG ) {
					Log.d(TAG, "add burst request for " + i + "th light image:");
					Log.d(TAG, "exposure: " + exposure);
				}
			}

			burst_exposures = requests;
			n_burst = requests.size();
		}

		if( frontscreen_flash ) {
			if( MyDebug.LOG )
				Log.d(TAG, "front screen flash");
			picture.onFrontScreenTurnOn();
				Handler handler = new Handler();
        	handler.postDelayed(new Runnable(){
        		@Override
        	    public void run(){
        			if( MyDebug.LOG )
        				Log.d(TAG, "take picture after delay for front screen flash");
        			if( camera != null ) {
        				takePictureNow(picture, error);
        			}
        	   }
        	}, 1000);
			return;
		}
		takePictureNow(picture, error);
	}
	
	public void setDisplayOrientation(int degrees) {

	    int result;
	    if( camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
	        result = (camera_info.orientation + degrees) % 360;
	        result = (360 - result) % 360;  // compensate the mirror
	    }
	    else {
	        result = (camera_info.orientation - degrees + 360) % 360;
	    }
		if( MyDebug.LOG ) {
			Log.d(TAG, "    info orientation is " + camera_info.orientation);
			Log.d(TAG, "    setDisplayOrientation to " + result);
		}

		try {
			camera.setDisplayOrientation(result);
		}
		catch(RuntimeException e) {
	    	// unclear why this happens, but have had crashes from Google Play...
			Log.e(TAG, "failed to set display orientation");
			e.printStackTrace();
		}
	    this.display_orientation = result;
	}
	
	public int getDisplayOrientation() {
		return this.display_orientation;
	}
	
	public int getCameraOrientation() {
		return camera_info.orientation;
	}
	
	public boolean isFrontFacing() {
		return (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
	}
	
	public void unlock() {
		this.stopPreview();
		camera.unlock();
	}
	
	@Override
	public void initVideoRecorderPrePrepare(MediaRecorder video_recorder) {
    	video_recorder.setCamera(camera);
	}
	
	@Override
	public void initVideoRecorderPostPrepare(MediaRecorder video_recorder, boolean want_photo_video_recording) throws CameraControllerException {

	}
	
	@Override
	public String getParametersString() {
		String string = "";
		try {
			string = this.getParameters().flatten();
		}
        catch(Exception e) {

    		if( MyDebug.LOG )
    			Log.e(TAG, "exception from getParameters().flatten()");
        	e.printStackTrace();
        }
		return string;
	}
}
