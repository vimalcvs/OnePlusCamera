package camera.vimal.vk;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class MyWidgetProviderTakePhoto extends AppWidgetProvider {
	private static final String TAG = "MyWidgetProviderTakePho";
	

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	if( MyDebug.LOG )
    		Log.d(TAG, "onUpdate");
    	if( MyDebug.LOG )
            Log.d(TAG, "length = " + appWidgetIds.length);

          for(int appWidgetId : appWidgetIds) {
        	if( MyDebug.LOG )
        		Log.d(TAG, "appWidgetId: " + appWidgetId);

            Intent intent = new Intent(context, TakePhoto.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);


            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_take_photo);
            views.setOnClickPendingIntent(R.id.widget_take_photo, pendingIntent);

                appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


}
