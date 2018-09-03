package camera.vimal.vk;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.View;

public class CanvasView extends View {
	private static final String TAG = "CanvasView";

	private final Preview preview;
	private final int [] measure_spec = new int[2];
	private final Handler handler = new Handler();
	private final Runnable tick;

	CanvasView(Context context, final Preview preview) {
		super(context);
		this.preview = preview;
		if( MyDebug.LOG ) {
			Log.d(TAG, "new CanvasView");
		}


		
		tick = new Runnable() {
		    public void run() {

				preview.test_ticker_called = true;
		        invalidate();
				handler.postDelayed(this, preview.getFrameRate());
		    }
		};
	}
	
	@Override
	public void onDraw(Canvas canvas) {

		preview.draw(canvas);
	}

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
		if( MyDebug.LOG )
			Log.d(TAG, "onMeasure: " + widthSpec + " x " + heightSpec);
    	preview.getMeasureSpec(measure_spec, widthSpec, heightSpec);
    	super.onMeasure(measure_spec[0], measure_spec[1]);
    }

	void onPause() {
		if( MyDebug.LOG )
			Log.d(TAG, "onPause()");
		handler.removeCallbacks(tick);
	}

	void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume()");
		tick.run();
	}
}
