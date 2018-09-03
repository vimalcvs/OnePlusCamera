package camera.vimal.vk;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



public abstract class CameraController {
	private static final String TAG = "CameraController";
	private final int cameraId;

	public static final String SCENE_MODE_DEFAULT = "auto";
	public static final String COLOR_EFFECT_DEFAULT = "none";
	public static final String WHITE_BALANCE_DEFAULT = "auto";
	public static final String ANTIBANDING_DEFAULT = "auto";
	public static final String ISO_DEFAULT = "auto";
	public static final long EXPOSURE_TIME_DEFAULT = 1000000000L/30;


	int count_camera_parameters_exception;
	public int count_precapture_timeout;
	public boolean test_wait_capture_result;
	public volatile int test_capture_results;
	public volatile int test_fake_flash_focus;
	public volatile int test_fake_flash_precapture;
	public volatile int test_fake_flash_photo;
	public volatile int test_af_state_null_focus;

	public static class CameraFeatures {
		public boolean is_zoom_supported;
		public int max_zoom;
		public List<Integer> zoom_ratios;
		public boolean supports_face_detection;
		public List<CameraController.Size> picture_sizes;
		public List<CameraController.Size> video_sizes;
		public List<CameraController.Size> video_sizes_high_speed;
		public List<CameraController.Size> preview_sizes;
		public List<String> supported_flash_values;
		public List<String> supported_focus_values;
		public int max_num_focus_areas;
		public float minimum_focus_distance;
		public int max_exposure;
		public float exposure_step;
		public boolean can_disable_shutter_sound;
		public int tonemap_max_curve_points;
		public boolean supports_tonemap_curve;
		public boolean supports_expo_bracketing;
		public int max_expo_bracketing_n_images;
		public boolean supports_raw;
		public boolean supports_burst;
		public float view_angle_x;
		public float view_angle_y;
		public boolean is_exposure_lock_supported;
		public boolean is_video_stabilization_supported;
		public boolean is_photo_video_recording_supported;
		public boolean supports_white_balance_temperature;
		public int min_temperature;
		public int max_temperature;
		public boolean supports_iso_range;
		public int min_iso;
		public int max_iso;
		public boolean supports_exposure_time;
		public long min_exposure_time;
		public long max_exposure_time;
		public int min_exposure;



		public static boolean supportsFrameRate(List<Size> sizes, int fps) {
			if( MyDebug.LOG )
				Log.d(TAG, "supportsFrameRate: " + fps);
			if( sizes == null )
				return false;
			for(Size size : sizes) {
				if( size.supportsFrameRate(fps) ) {
					if( MyDebug.LOG )
						Log.d(TAG, "fps is supported");
					return true;
				}
			}
			if( MyDebug.LOG )
				Log.d(TAG, "fps is NOT supported");
			return false;
		}

		public static Size findSize(List<Size> sizes, Size size, double fps, boolean return_closest) {
			Size last_s = null;
			for(Size s : sizes) {
				if (size.equals(s)) {
					last_s = s;
					if (fps > 0) {
						if (s.supportsFrameRate(fps)) {
							return s;
						}
					} else {
						return s;
					}
				}
			}
			return return_closest ? last_s : null;
		}
	}


	public static class RangeSorter implements Comparator<int[]>, Serializable {
		private static final long serialVersionUID = 6064542819837033805L;
		@Override
		public int compare(int[] o1, int[] o2) {
			if (o1[0] == o2[0]) return o1[1] - o2[1];
			return o1[0] - o2[0];
		}
	}


	public static class SizeSorter implements Comparator<Size>, Serializable {
		private static final long serialVersionUID = 6064542819837033805L;

		@Override
		public int compare(final CameraController.Size a, final CameraController.Size b) {
			return b.width * b.height - a.width * a.height;
		}
	}

	public static class Size {
		public final int width;
		public final int height;
		final List<int[]> fps_ranges;
		public final boolean high_speed;

		Size(int width, int height, List<int[]> fps_ranges, boolean high_speed) {
			this.width = width;
			this.height = height;
			this.fps_ranges = fps_ranges;
			this.high_speed = high_speed;
			Collections.sort(this.fps_ranges, new RangeSorter());
		}

		public Size(int width, int height) {
			this(width, height, new ArrayList<int[]>(), false);
		}

		boolean supportsFrameRate(double fps) {
			for (int[] f : this.fps_ranges) {
				if (f[0] <= fps && fps <= f[1])
					return true;
			}
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if( !(o instanceof Size) )
				return false;
			Size that = (Size)o;
			return this.width == that.width && this.height == that.height;
		}
		
		@Override
		public int hashCode() {

			return width*31 + height;
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			for (int[] f : this.fps_ranges) {
				s.append(" [").append(f[0]).append("-").append(f[1]).append("]");
			}
			return this.width + "x" + this.height + " " + s + (this.high_speed ? "-hs" : "");
		}
	}
	

	public static class Area {
		final Rect rect;
		final int weight;
		
		public Area(Rect rect, int weight) {
			this.rect = rect;
			this.weight = weight;
		}
	}
	
	public interface FaceDetectionListener {
		void onFaceDetection(Face[] faces);
	}
	
	public interface PictureCallback {
		void onStarted();
		void onCompleted();
		void onPictureTaken(byte[] data);

		void onRawPictureTaken(RawImage raw_image);

		void onBurstPictureTaken(List<byte[]> images);

		void onFrontScreenTurnOn();
	}
	
	public interface AutoFocusCallback {
		void onAutoFocus(boolean success);
	}
	
	public interface ContinuousFocusMoveCallback {
		void onContinuousFocusMove(boolean start);
	}
	
	public interface ErrorCallback {
		void onError();
	}
	
	public static class Face {
		public final int score;

		public final Rect rect;

		Face(int score, Rect rect) {
			this.score = score;
			this.rect = rect;
		}
	}
	
	public static class SupportedValues {
		public final List<String> values;
		public final String selected_value;
		SupportedValues(List<String> values, String selected_value) {
			this.values = values;
			this.selected_value = selected_value;
		}
	}

	public abstract void release();
	public abstract void onError();
	CameraController(int cameraId) {
		this.cameraId = cameraId;
	}
	public abstract String getAPI();
	public abstract CameraFeatures getCameraFeatures() throws CameraControllerException;
	public int getCameraId() {
		return cameraId;
	}


	public boolean shouldCoverPreview() {
		return false;
	}
	public abstract SupportedValues setSceneMode(String value);
	public abstract String getSceneMode();
	public abstract boolean sceneModeAffectsFunctionality();
	public abstract SupportedValues setColorEffect(String value);
	public abstract String getColorEffect();
	public abstract SupportedValues setWhiteBalance(String value);
	public abstract String getWhiteBalance();
	public abstract boolean setWhiteBalanceTemperature(int temperature);
	public abstract int getWhiteBalanceTemperature();
	public abstract SupportedValues setAntiBanding(String value);
	public abstract String getAntiBanding();
	public abstract SupportedValues setISO(String value);
	public abstract void setManualISO(boolean manual_iso, int iso);
	public abstract boolean isManualISO();
	public abstract boolean setISO(int iso);
    public abstract String getISOKey();
	public abstract int getISO();
	public abstract long getExposureTime();
	public abstract boolean setExposureTime(long exposure_time);
    public abstract CameraController.Size getPictureSize();
    public abstract void setPictureSize(int width, int height);
    public abstract CameraController.Size getPreviewSize();
    public abstract void setPreviewSize(int width, int height);
	public abstract void setWantBurst(boolean want_burst);
	public abstract void setBurstNImages(int burst_requested_n_images);
	public abstract void setBurstForNoiseReduction(boolean burst_for_noise_reduction);
	public abstract void setExpoBracketing(boolean want_expo_bracketing);
	public abstract void setExpoBracketingNImages(int n_images);
	public abstract void setExpoBracketingStops(double stops);
	public abstract void setUseExpoFastBurst(boolean use_expo_fast_burst);
	public abstract void setOptimiseAEForDRO(boolean optimise_ae_for_dro);
	public abstract void setRaw(boolean want_raw, int max_raw_images);
	public abstract void setVideoHighSpeed(boolean setVideoHighSpeed);
	public void setUseCamera2FakeFlash(boolean use_fake_precapture) {
	}
	public boolean getUseCamera2FakeFlash() {
		return false;
	}
	public abstract void setVideoStabilization(boolean enabled);
	public abstract boolean getVideoStabilization();
	public abstract void setLogProfile(boolean use_log_profile, float log_profile_strength);
	public abstract boolean isLogProfile();
	public abstract int getJpegQuality();
	public abstract void setJpegQuality(int quality);
	public abstract int getZoom();
	public abstract void setZoom(int value);
	public abstract int getExposureCompensation();
	public abstract boolean setExposureCompensation(int new_exposure);
	public abstract void setPreviewFpsRange(int min, int max);
	public abstract List<int []> getSupportedPreviewFpsRange();

	public abstract void setFocusValue(String focus_value);
	public abstract String getFocusValue();
	public abstract float getFocusDistance();
	public abstract boolean setFocusDistance(float focus_distance);
	public abstract void setFlashValue(String flash_value);
	public abstract String getFlashValue();
	public abstract void setRecordingHint(boolean hint);
	public abstract void setAutoExposureLock(boolean enabled);
	public abstract boolean getAutoExposureLock();
	public abstract void setRotation(int rotation);
	public abstract void setLocationInfo(Location location);
	public abstract void removeLocationInfo();
	public abstract void enableShutterSound(boolean enabled);
	public abstract boolean setFocusAndMeteringArea(List<CameraController.Area> areas);
	public abstract void clearFocusAndMetering();
	public abstract List<CameraController.Area> getFocusAreas();
	public abstract List<CameraController.Area> getMeteringAreas();
	public abstract boolean supportsAutoFocus();
	public abstract boolean focusIsContinuous();
	public abstract boolean focusIsVideo();
	public abstract void reconnect() throws CameraControllerException;
	public abstract void setPreviewDisplay(SurfaceHolder holder) throws CameraControllerException;
	public abstract void setPreviewTexture(SurfaceTexture texture) throws CameraControllerException;
	public abstract void startPreview() throws CameraControllerException;
	public abstract void stopPreview();
	public abstract boolean startFaceDetection();
	public abstract void setFaceDetectionListener(final CameraController.FaceDetectionListener listener);
	public abstract void autoFocus(final CameraController.AutoFocusCallback cb, boolean capture_follows_autofocus_hint);
	public abstract void setCaptureFollowAutofocusHint(boolean capture_follows_autofocus_hint);
	public abstract void cancelAutoFocus();
	public abstract void setContinuousFocusMoveCallback(ContinuousFocusMoveCallback cb);
	public abstract void takePicture(final CameraController.PictureCallback picture, final ErrorCallback error);
	public abstract void setDisplayOrientation(int degrees);
	public abstract int getDisplayOrientation();
	public abstract int getCameraOrientation();
	public abstract boolean isFrontFacing();
	public abstract void unlock();
	public abstract void initVideoRecorderPrePrepare(MediaRecorder video_recorder);
	public abstract void initVideoRecorderPostPrepare(MediaRecorder video_recorder, boolean want_photo_video_recording) throws CameraControllerException;
	public abstract String getParametersString();
	public boolean captureResultIsAEScanning() {
		return false;
	}
	public boolean needsFlash() {
		return false;
	}
	public boolean needsFrontScreenFlash() {
		return false;
	}
	public boolean captureResultHasWhiteBalanceTemperature() {
		return false;
	}
	public int captureResultWhiteBalanceTemperature() {
		return 0;
	}
	public boolean captureResultHasIso() {
		return false;
	}
	public int captureResultIso() {
		return 0;
	}
	public boolean captureResultHasExposureTime() {
		return false;
	}
	public long captureResultExposureTime() {
		return 0;
	}
	SupportedValues checkModeIsSupported(List<String> values, String value, String default_value) {
		if( values != null && values.size() > 1 ) {
			if( MyDebug.LOG ) {
				for(int i=0;i<values.size();i++) {
		        	Log.d(TAG, "supported value: " + values.get(i));
				}
			}

			if( !values.contains(value) ) {
				if( MyDebug.LOG )
					Log.d(TAG, "value not valid!");
				if( values.contains(default_value) )
					value = default_value;
				else
					value = values.get(0);
				if( MyDebug.LOG )
					Log.d(TAG, "value is now: " + value);
			}
			return new SupportedValues(values, value);
		}
		return null;
	}
}
