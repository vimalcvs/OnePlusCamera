package camera.vimal.vk;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.view.View;


public interface CameraSurface {
	View getView();
	void setPreviewDisplay(CameraController camera_controller); // n.b., uses double-dispatch similar to Visitor pattern - behaviour depends on type of CameraSurface and CameraController
	void setVideoRecorder(MediaRecorder video_recorder);
	void setTransform(Matrix matrix);
	void onPause();
	void onResume();
}
