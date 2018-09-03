package camera.vimal.vk;;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SaveLocationHistory {
	private static final String TAG = "SaveLocationHistory";
	private final MainActivity main_activity;
	private final String pref_base;
	private final ArrayList<String> save_location_history = new ArrayList<>();


	SaveLocationHistory(MainActivity main_activity, String pref_base, String folder_name) {
		if( MyDebug.LOG )
			Log.d(TAG, "pref_base: " + pref_base);
		this.main_activity = main_activity;
		this.pref_base = pref_base;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);

		// read save locations
        save_location_history.clear();
        int save_location_history_size = sharedPreferences.getInt(pref_base + "_size", 0);
		if( MyDebug.LOG )
			Log.d(TAG, "save_location_history_size: " + save_location_history_size);
        for(int i=0;i<save_location_history_size;i++) {
        	String string = sharedPreferences.getString(pref_base + "_" + i, null);
        	if( string != null ) {
    			if( MyDebug.LOG )
    				Log.d(TAG, "save_location_history " + i + ": " + string);
        		save_location_history.add(string);
        	}
        }
        // also update, just in case a new folder has been set
		updateFolderHistory(folder_name, false); // update_icon can be false, as updateGalleryIcon() is called later in MainActivity.onResume()

	}


    void updateFolderHistory(String folder_name, boolean update_icon) {
		updateFolderHistory(folder_name);
		if( update_icon ) {
			main_activity.updateGalleryIcon(); // if the folder has changed, need to update the gallery icon
		}
    }


    private void updateFolderHistory(String folder_name) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "updateFolderHistory: " + folder_name);
			Log.d(TAG, "save_location_history size: " + save_location_history.size());
			for(int i=0;i<save_location_history.size();i++) {
				Log.d(TAG, save_location_history.get(i));
			}
		}
		while( save_location_history.remove(folder_name) ) {
		}
		save_location_history.add(folder_name);
		while( save_location_history.size() > 6 ) {
			save_location_history.remove(0);
		}
		writeSaveLocations();
		if( MyDebug.LOG ) {
			Log.d(TAG, "updateFolderHistory exit:");
			Log.d(TAG, "save_location_history size: " + save_location_history.size());
			for(int i=0;i<save_location_history.size();i++) {
				Log.d(TAG, save_location_history.get(i));
			}
		}
    }
    

    void clearFolderHistory(String folder_name) {
		if( MyDebug.LOG )
			Log.d(TAG, "clearFolderHistory: " + folder_name);
		save_location_history.clear();
		updateFolderHistory(folder_name, true); // to re-add the current choice, and save
    }


    private void writeSaveLocations() {
		if( MyDebug.LOG )
			Log.d(TAG, "writeSaveLocations");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(pref_base + "_size", save_location_history.size());
		if( MyDebug.LOG )
			Log.d(TAG, "save_location_history_size = " + save_location_history.size());
        for(int i=0;i<save_location_history.size();i++) {
        	String string = save_location_history.get(i);
    		editor.putString(pref_base + "_" + i, string);
        }
		editor.apply();
    }


    public int size() {
    	return save_location_history.size();
    }


    public String get(int index) {
    	return save_location_history.get(index);
    }
    

    public boolean contains(String value) {
    	return save_location_history.contains(value);
    }
}
