package camera.vimal.vk;



import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class MyWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "MyWidgetProvider";
	

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	if( MyDebug.LOG )
    		Log.d(TAG, "onUpdate");
    	if( MyDebug.LOG )
    		Log.d(TAG, "length = " + appWidgetIds.length);


		for(int appWidgetId : appWidgetIds) {
        	if( MyDebug.LOG )
        		Log.d(TAG, "appWidgetId: " + appWidgetId);

            PendingIntent pendingIntent;
           {

	            Intent intent = new Intent(context, MainActivity.class);
	            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			}


            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget_launch_open_camera, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


}
