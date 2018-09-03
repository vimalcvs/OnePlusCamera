package camera.vimal.vk;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class MyPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "MyPreferenceFragment";

	private int cameraId;

	private final HashSet<AlertDialog> dialogs = new HashSet<>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if( MyDebug.LOG )
			Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final Bundle bundle = getArguments();
		this.cameraId = bundle.getInt("cameraId");
		if( MyDebug.LOG )
			Log.d(TAG, "cameraId: " + cameraId);
		final int nCameras = bundle.getInt("nCameras");
		if( MyDebug.LOG )
			Log.d(TAG, "nCameras: " + nCameras);

		final String camera_api = bundle.getString("camera_api");
		
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

		final boolean supports_auto_stabilise = bundle.getBoolean("supports_auto_stabilise");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_auto_stabilise: " + supports_auto_stabilise);


		boolean has_antibanding = false;
		String [] antibanding_values = bundle.getStringArray("antibanding");
		if( antibanding_values != null && antibanding_values.length > 0 ) {
			String [] antibanding_entries = bundle.getStringArray("antibanding_entries");
			if( antibanding_entries != null && antibanding_entries.length == antibanding_values.length ) { // should always be true here, but just in case
				readFromBundle(antibanding_values, antibanding_entries, PreferenceKeys.AntiBandingPreferenceKey, CameraController.ANTIBANDING_DEFAULT, "preference_category_camera_quality");
				has_antibanding = true;
			}
		}
		if( MyDebug.LOG )
			Log.d(TAG, "has_antibanding?: " + has_antibanding);
		if( !has_antibanding ) {
			Preference pref = findPreference("preference_antibanding");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_camera_quality");
        	pg.removePreference(pref);
		}

		final boolean supports_face_detection = bundle.getBoolean("supports_face_detection");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_face_detection: " + supports_face_detection);

		if( !supports_face_detection ) {
			Preference pref = findPreference("preference_face_detection");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_camera_controls");
        	pg.removePreference(pref);
		}

		final int preview_width = bundle.getInt("preview_width");
		final int preview_height = bundle.getInt("preview_height");
		final int [] preview_widths = bundle.getIntArray("preview_widths");
		final int [] preview_heights = bundle.getIntArray("preview_heights");
		final int [] video_widths = bundle.getIntArray("video_widths");
		final int [] video_heights = bundle.getIntArray("video_heights");
		final int [] video_fps = bundle.getIntArray("video_fps");

		final int resolution_width = bundle.getInt("resolution_width");
		final int resolution_height = bundle.getInt("resolution_height");
		final int [] widths = bundle.getIntArray("resolution_widths");
		final int [] heights = bundle.getIntArray("resolution_heights");
		if( widths != null && heights != null ) {
			CharSequence [] entries = new CharSequence[widths.length];
			CharSequence [] values = new CharSequence[widths.length];
			for(int i=0;i<widths.length;i++) {
				entries[i] = widths[i] + " x " + heights[i] + " " + Preview.getAspectRatioMPString(widths[i], heights[i]);
				values[i] = widths[i] + " " + heights[i];
			}
			ListPreference lp = (ListPreference)findPreference("preference_resolution");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String resolution_preference_key = PreferenceKeys.getResolutionPreferenceKey(cameraId);
			String resolution_value = sharedPreferences.getString(resolution_preference_key, "");
			if( MyDebug.LOG )
				Log.d(TAG, "resolution_value: " + resolution_value);
			lp.setValue(resolution_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(resolution_preference_key);
		}
		else {
			Preference pref = findPreference("preference_resolution");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		String fps_preference_key = PreferenceKeys.getVideoFPSPreferenceKey(cameraId);
		if( MyDebug.LOG )
			Log.d(TAG, "fps_preference_key: " + fps_preference_key);
		String fps_value = sharedPreferences.getString(fps_preference_key, "default");
		if( MyDebug.LOG )
			Log.d(TAG, "fps_value: " + fps_value);
		if( video_fps != null ) {
			// build video fps settings
			CharSequence [] entries = new CharSequence[video_fps.length+1];
			CharSequence [] values = new CharSequence[video_fps.length+1];
			int i=0;
			// default:
			entries[i] = getResources().getString(R.string.preference_video_fps_default);
			values[i] = "default";
			i++;
			for(int fps : video_fps) {
				entries[i] = "" + fps;
				values[i] = "" + fps;
				i++;
			}

			ListPreference lp = (ListPreference)findPreference("preference_video_fps");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			lp.setValue(fps_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(fps_preference_key);
		}

		{
			final int n_quality = 100;
			CharSequence [] entries = new CharSequence[n_quality];
			CharSequence [] values = new CharSequence[n_quality];
			for(int i=0;i<n_quality;i++) {
				entries[i] = "" + (i+1) + "%";
				values[i] = "" + (i+1);
			}
			ListPreference lp = (ListPreference)findPreference("preference_quality");
			lp.setEntries(entries);
			lp.setEntryValues(values);
		}
		
		final boolean supports_raw = bundle.getBoolean("supports_raw");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_raw: " + supports_raw);

		if( !supports_raw ) {
			Preference pref = findPreference("preference_raw");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}
		else {
        	ListPreference pref = (ListPreference)findPreference("preference_raw");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
	        	// RAW only mode requires at least Android 7; earlier versions seem to have poorer support for DNG files
	        	pref.setEntries(R.array.preference_raw_entries_preandroid7);
	        	pref.setEntryValues(R.array.preference_raw_values_preandroid7);
			}

        	pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        		@Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
            		if( MyDebug.LOG )
            			Log.d(TAG, "clicked raw: " + newValue);
            		if( newValue.equals("preference_raw_yes") || newValue.equals("preference_raw_only") ) {
            			// we check done_raw_info every time, so that this works if the user selects RAW again without leaving and returning to Settings
            			boolean done_raw_info = sharedPreferences.contains(PreferenceKeys.RawInfoPreferenceKey);
            			if( !done_raw_info ) {
	        		        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
	        	            alertDialog.setTitle(R.string.preference_raw);
	        	            alertDialog.setMessage(R.string.raw_info);
	        	            alertDialog.setPositiveButton(android.R.string.ok, null);
	        	            alertDialog.setNegativeButton(R.string.dont_show_again, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
				            		if( MyDebug.LOG )
				            			Log.d(TAG, "user clicked dont_show_again for raw info dialog");
				            		SharedPreferences.Editor editor = sharedPreferences.edit();
				            		editor.putBoolean(PreferenceKeys.RawInfoPreferenceKey, true);
				            		editor.apply();
								}
	        	            });
							final AlertDialog alert = alertDialog.create();
							// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
							alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface arg0) {
									if( MyDebug.LOG )
										Log.d(TAG, "raw dialog dismissed");
									dialogs.remove(alert);
								}
							});
							alert.show();
	        	            dialogs.add(alert);
            			}
                    }
                	return true;
                }
            });        	
		}

		final boolean supports_hdr = bundle.getBoolean("supports_hdr");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_hdr: " + supports_hdr);

		if( !supports_hdr ) {
			Preference pref = findPreference("preference_hdr_save_expo");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		final boolean supports_expo_bracketing = bundle.getBoolean("supports_expo_bracketing");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_expo_bracketing: " + supports_expo_bracketing);

		final int max_expo_bracketing_n_images = bundle.getInt("max_expo_bracketing_n_images");
		if( MyDebug.LOG )
			Log.d(TAG, "max_expo_bracketing_n_images: " + max_expo_bracketing_n_images);

		final boolean supports_nr = bundle.getBoolean("supports_nr");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_nr: " + supports_nr);

		if( !supports_nr ) {
			Preference pref = findPreference("preference_nr_save");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		final boolean supports_exposure_compensation = bundle.getBoolean("supports_exposure_compensation");
		final int exposure_compensation_min = bundle.getInt("exposure_compensation_min");
		final int exposure_compensation_max = bundle.getInt("exposure_compensation_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_exposure_compensation: " + supports_exposure_compensation);
			Log.d(TAG, "exposure_compensation_min: " + exposure_compensation_min);
			Log.d(TAG, "exposure_compensation_max: " + exposure_compensation_max);
		}

		final boolean supports_iso_range = bundle.getBoolean("supports_iso_range");
		final int iso_range_min = bundle.getInt("iso_range_min");
		final int iso_range_max = bundle.getInt("iso_range_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_iso_range: " + supports_iso_range);
			Log.d(TAG, "iso_range_min: " + iso_range_min);
			Log.d(TAG, "iso_range_max: " + iso_range_max);
		}

		final boolean supports_exposure_time = bundle.getBoolean("supports_exposure_time");
		final long exposure_time_min = bundle.getLong("exposure_time_min");
		final long exposure_time_max = bundle.getLong("exposure_time_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_exposure_time: " + supports_exposure_time);
			Log.d(TAG, "exposure_time_min: " + exposure_time_min);
			Log.d(TAG, "exposure_time_max: " + exposure_time_max);
		}

		final boolean supports_white_balance_temperature = bundle.getBoolean("supports_white_balance_temperature");
		final int white_balance_temperature_min = bundle.getInt("white_balance_temperature_min");
		final int white_balance_temperature_max = bundle.getInt("white_balance_temperature_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_white_balance_temperature: " + supports_white_balance_temperature);
			Log.d(TAG, "white_balance_temperature_min: " + white_balance_temperature_min);
			Log.d(TAG, "white_balance_temperature_max: " + white_balance_temperature_max);
		}

		if( !supports_expo_bracketing || max_expo_bracketing_n_images <= 3 ) {
			Preference pref = findPreference("preference_expo_bracketing_n_images");
			PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_photo_settings");
			pg.removePreference(pref);
		}
		if( !supports_expo_bracketing ) {
			Preference pref = findPreference("preference_expo_bracketing_stops");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		final String [] video_quality = bundle.getStringArray("video_quality");
		final String [] video_quality_string = bundle.getStringArray("video_quality_string");
		if( video_quality != null && video_quality_string != null ) {
			CharSequence [] entries = new CharSequence[video_quality.length];
			CharSequence [] values = new CharSequence[video_quality.length];
			for(int i=0;i<video_quality.length;i++) {
				entries[i] = video_quality_string[i];
				values[i] = video_quality[i];
			}
			ListPreference lp = (ListPreference)findPreference("preference_video_quality");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String video_quality_preference_key = bundle.getString("video_quality_preference_key");
			if( MyDebug.LOG )
				Log.d(TAG, "video_quality_preference_key: " + video_quality_preference_key);
			String video_quality_value = sharedPreferences.getString(video_quality_preference_key, "");
			if( MyDebug.LOG )
				Log.d(TAG, "video_quality_value: " + video_quality_value);
					lp.setKey(video_quality_preference_key);
			lp.setValue(video_quality_value);
		}
		else {
			Preference pref = findPreference("preference_video_quality");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		final String current_video_quality = bundle.getString("current_video_quality");
		final int video_frame_width = bundle.getInt("video_frame_width");
		final int video_frame_height = bundle.getInt("video_frame_height");
		final int video_bit_rate = bundle.getInt("video_bit_rate");
		final int video_frame_rate = bundle.getInt("video_frame_rate");
		final double video_capture_rate = bundle.getDouble("video_capture_rate");
		final boolean video_high_speed = bundle.getBoolean("video_high_speed");
		final float video_capture_rate_factor = bundle.getFloat("video_capture_rate_factor");

		final boolean supports_force_video_4k = bundle.getBoolean("supports_force_video_4k");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_force_video_4k: " + supports_force_video_4k);
		if( !supports_force_video_4k || video_quality == null ) {
			Preference pref = findPreference("preference_force_video_4k");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_video_debugging");
        	pg.removePreference(pref);
		}
		
		final boolean supports_video_stabilization = bundle.getBoolean("supports_video_stabilization");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_video_stabilization: " + supports_video_stabilization);
		if( !supports_video_stabilization ) {
			Preference pref = findPreference("preference_video_stabilization");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		{
        	ListPreference pref = (ListPreference)findPreference("preference_record_audio_src");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
	        	pref.setEntries(R.array.preference_record_audio_src_entries_preandroid7);
	        	pref.setEntryValues(R.array.preference_record_audio_src_values_preandroid7);
			}
		}

		final boolean can_disable_shutter_sound = bundle.getBoolean("can_disable_shutter_sound");
		if( MyDebug.LOG )
			Log.d(TAG, "can_disable_shutter_sound: " + can_disable_shutter_sound);
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !can_disable_shutter_sound ) {
        	// Camera.enableShutterSound requires JELLY_BEAN_MR1 or greater
        	Preference pref = findPreference("preference_shutter_sound");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_camera_controls_more");
        	pg.removePreference(pref);
        }

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {
        	// Some immersive modes require KITKAT - simpler to require Kitkat for any of the menu options
        	Preference pref = findPreference("preference_immersive_mode");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_gui");
        	pg.removePreference(pref);
        }
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
        	// the required ExifInterface tags requires Android N or greater
        	Preference pref = findPreference("preference_category_exif_tags");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
        }
        else {
			setSummary("preference_exif_artist");
			setSummary("preference_exif_copyright");
		}


		final boolean using_android_l = bundle.getBoolean("using_android_l");
		if( MyDebug.LOG )
			Log.d(TAG, "using_android_l: " + using_android_l);
		final boolean supports_photo_video_recording = bundle.getBoolean("supports_photo_video_recording");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_photo_video_recording: " + supports_photo_video_recording);

        if( !using_android_l ) {
        	Preference pref = findPreference("preference_show_iso");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_gui");
        	pg.removePreference(pref);
        }

        if( !using_android_l ) {
        	Preference pref = findPreference("preference_camera2_fake_flash");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
        	pg.removePreference(pref);

			pref = findPreference("preference_camera2_fast_burst");
			pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			pg.removePreference(pref);

			pref = findPreference("preference_camera2_photo_video_recording");
			pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			pg.removePreference(pref);
        }
        else {
        	if( !supports_photo_video_recording ) {
				Preference pref = findPreference("preference_camera2_photo_video_recording");
				PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
				pg.removePreference(pref);
			}
		}

		final int tonemap_max_curve_points = bundle.getInt("tonemap_max_curve_points");
		final boolean supports_tonemap_curve = bundle.getBoolean("supports_tonemap_curve");
		if( MyDebug.LOG ) {
			Log.d(TAG, "tonemap_max_curve_points: " + tonemap_max_curve_points);
			Log.d(TAG, "supports_tonemap_curve: " + supports_tonemap_curve);
		}
        if( !supports_tonemap_curve ) {
        	Preference pref = findPreference("preference_video_log");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		{
			// remove preference_category_photo_debugging category if empty (which will be the case for old api)
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			if( MyDebug.LOG )
				Log.d(TAG, "preference_category_photo_debugging children: " + pg.getPreferenceCount());
        	if( pg.getPreferenceCount() == 0 ) {
        		// pg.getParent() requires API level 26
	        	PreferenceGroup parent = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        		parent.removePreference(pg);
			}
		}

		final boolean supports_camera2 = bundle.getBoolean("supports_camera2");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_camera2: " + supports_camera2);
        if( supports_camera2 ) {
        	final Preference pref = findPreference("preference_use_camera2");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_use_camera2") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked camera2 API - need to restart");
                		restartOnePlusCamera();
	                	return false;
                	}
                	return false;
                }
            });
        }
        else {
        	Preference pref = findPreference("preference_use_camera2");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_online");
        	pg.removePreference(pref);
        }
        
        {
            final Preference pref = findPreference("preference_online_help");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_online_help") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked online help");
	            		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
						main_activity.launchOnlineHelp();
                		return false;
                	}
                	return false;
                }
            });
        }

        {
        	ListPreference pref = (ListPreference)findPreference("preference_ghost_image");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {

	        	pref.setEntries(R.array.preference_ghost_image_entries_preandroid5);
	        	pref.setEntryValues(R.array.preference_ghost_image_values_preandroid5);
			}

        	pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        		@Override
                public boolean onPreferenceChange(Preference arg0, Object newValue) {
            		if( MyDebug.LOG )
            			Log.d(TAG, "clicked ghost image: " + newValue);
            		if( newValue.equals("preference_ghost_image_selected") ) {
						MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
						main_activity.openGhostImageChooserDialogSAF(true);
					}
            		return true;
                }
            });
        }


        {
        	Preference pref = findPreference("preference_save_location");
        	pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		@Override
                public boolean onPreferenceClick(Preference arg0) {
            		if( MyDebug.LOG )
            			Log.d(TAG, "clicked save location");
            		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
            		if( main_activity.getStorageUtils().isUsingSAF() ) {
                		main_activity.openFolderChooserDialogSAF(true);
            			return true;
                    }
            		else {
						FolderChooserDialog fragment = new SaveFolderChooserDialog();
                		fragment.show(getFragmentManager(), "FOLDER_FRAGMENT");
                    	return true;
            		}
                }
            });        	
        }

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
        	Preference pref = findPreference("preference_using_saf");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_camera_controls_more");
        	pg.removePreference(pref);
        }
        else {
            final Preference pref = findPreference("preference_using_saf");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_using_saf") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked saf");
            			if( sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) ) {
                    		if( MyDebug.LOG )
                    			Log.d(TAG, "saf is now enabled");
                    			{
                        		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
                    			Toast.makeText(main_activity, R.string.saf_select_save_location, Toast.LENGTH_SHORT).show();
                        		main_activity.openFolderChooserDialogSAF(true);
                    		}
            			}
            			else {
                    		if( MyDebug.LOG )
                    			Log.d(TAG, "saf is now disabled");
            			}
                	}
                	return false;
                }
            });
        }

		{
			final Preference pref = findPreference("preference_calibrate_level");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_calibrate_level") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked calibrate level option");
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
						alertDialog.setTitle(getActivity().getResources().getString(R.string.preference_calibrate_level));
						alertDialog.setMessage(R.string.preference_calibrate_level_dialog);
						alertDialog.setPositiveButton(R.string.preference_calibrate_level_calibrate, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if( MyDebug.LOG )
									Log.d(TAG, "user clicked calibrate level");
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								if( main_activity.getPreview().hasLevelAngle() ) {
									double current_level_angle = main_activity.getPreview().getLevelAngleUncalibrated();
									SharedPreferences.Editor editor = sharedPreferences.edit();
									editor.putFloat(PreferenceKeys.CalibratedLevelAnglePreferenceKey, (float)current_level_angle);
									editor.apply();
									main_activity.getPreview().updateLevelAngles();
									Toast.makeText(main_activity, R.string.preference_calibrate_level_calibrated, Toast.LENGTH_SHORT).show();
								}
							}
						});
						alertDialog.setNegativeButton(R.string.preference_calibrate_level_reset, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if( MyDebug.LOG )
									Log.d(TAG, "user clicked reset calibration level");
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								SharedPreferences.Editor editor = sharedPreferences.edit();
								editor.putFloat(PreferenceKeys.CalibratedLevelAnglePreferenceKey, 0.0f);
								editor.apply();
								main_activity.getPreview().updateLevelAngles();
								Toast.makeText(main_activity, R.string.preference_calibrate_level_calibration_reset, Toast.LENGTH_SHORT).show();
							}
						});
						final AlertDialog alert = alertDialog.create();

						alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface arg0) {
								if( MyDebug.LOG )
									Log.d(TAG, "calibration dialog dismissed");
								dialogs.remove(alert);
							}
						});
						alert.show();
						dialogs.add(alert);
						return false;
					}
					return false;
				}
			});
		}

        {
            final Preference pref = findPreference("preference_donate");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_donate") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked to donate");
            	        /*Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.getDonateMarketLink()));
            	        try {
            	        	startActivity(browserIntent);
            	        }
            			catch(ActivityNotFoundException e) {
            				// needed in case market:// not supported
            				if( MyDebug.LOG )
            					Log.d(TAG, "can't launch market:// intent");
                	        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.getDonateLink()));
            	        	startActivity(browserIntent);
            			}*/
            	        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.getDonateLink()));
        	        	startActivity(browserIntent);
                		return false;
                	}
                	return false;
                }
            });
        }

        {
            final Preference pref = findPreference("preference_about");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_about") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked about");
            	        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
                        alertDialog.setTitle(R.string.preference_about);
                        final StringBuilder about_string = new StringBuilder();
                		final String gpl_link = "";
                		final String online_help_link = "check here";
                        String version = "UNKNOWN_VERSION";
                        int version_code = -1;
						try {
	                        PackageInfo pInfo = MyPreferenceFragment.this.getActivity().getPackageManager().getPackageInfo(MyPreferenceFragment.this.getActivity().getPackageName(), 0);
	                        version = pInfo.versionName;
	                        version_code = pInfo.versionCode;
	                    }
						catch(NameNotFoundException e) {
	                		if( MyDebug.LOG )
	                			Log.d(TAG, "NameNotFoundException exception trying to get version number");
							e.printStackTrace();
						}
                        about_string.append("OnePlus Camera ");
                        about_string.append(version);
                        about_string.append("\nCode: ");
                        about_string.append(version_code);
                        about_string.append("\n(c) 2018-2019 ");

						about_string.append("\nDeveloper: Vimal K. Vishwakarma");

                        about_string.append("\nMore Apps: ");
                        about_string.append(gpl_link);
                        about_string.append(""+ online_help_link + "");
                        about_string.append("\nPackage: ");
                        about_string.append(MyPreferenceFragment.this.getActivity().getPackageName());
                        about_string.append("\nAndroid API version: ");
                        about_string.append(Build.VERSION.SDK_INT);
                        about_string.append("\nDevice manufacturer: ");
                        about_string.append(Build.MANUFACTURER);
                        about_string.append("\nDevice model: ");
                        about_string.append(Build.MODEL);
                        about_string.append("\nDevice code-name: ");
                        about_string.append(Build.HARDWARE);
                        about_string.append("\nDevice variant: ");
                        about_string.append(Build.DEVICE);


                		SpannableString span = new SpannableString(about_string);
					    span.setSpan(new ClickableSpan() {
					        @Override
        					public void onClick(View v) {
                        		if( MyDebug.LOG )
	            					Log.d(TAG, "gpl link clicked");
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=6064542819837033805"));
								startActivity(browserIntent);
					        }
						}, about_string.indexOf(gpl_link), about_string.indexOf(gpl_link) + gpl_link.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
					    span.setSpan(new ClickableSpan() {
					        @Override
        					public void onClick(View v) {
                        		if( MyDebug.LOG )
	            					Log.d(TAG, "online help link clicked");
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								main_activity.launchOnlineHelp();
					        }
						}, about_string.indexOf(online_help_link), about_string.indexOf(online_help_link) + online_help_link.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

					  	final float scale = getActivity().getResources().getDisplayMetrics().density;
						TextView textView = new TextView(getActivity());
						textView.setText(span);
						textView.setMovementMethod(LinkMovementMethod.getInstance());
						textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
						ScrollView scrollView = new ScrollView(getActivity());
						scrollView.addView(textView);
						// padding values from /sdk/platforms/android-18/data/res/layout/alert_dialog.xml
						textView.setPadding((int)(5*scale+0.5f), (int)(5*scale+0.5f), (int)(5*scale+0.5f), (int)(5*scale+0.5f));
						scrollView.setPadding((int)(14*scale+0.5f), (int)(2*scale+0.5f), (int)(10*scale+0.5f), (int)(12*scale+0.5f));
						alertDialog.setView(scrollView);
                        //alertDialog.setMessage(about_string);

                        alertDialog.setPositiveButton(android.R.string.ok, null);
                        alertDialog.setNegativeButton(R.string.about_copy_to_clipboard, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                        		if( MyDebug.LOG )
                        			Log.d(TAG, "user clicked copy to clipboard");
							 	ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE); 
							 	ClipData clip = ClipData.newPlainText("OnePlus Camera About", about_string);
							 	clipboard.setPrimaryClip(clip);
                            }
                        });
						final AlertDialog alert = alertDialog.create();
						// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
						alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface arg0) {
								if( MyDebug.LOG )
									Log.d(TAG, "about dialog dismissed");
								dialogs.remove(alert);
							}
						});
						alert.show();
						dialogs.add(alert);
                		return false;
                	}
                	return false;
                }
            });
        }

        {
            final Preference pref = findPreference("preference_reset");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_reset") ) {
                		if( MyDebug.LOG )
                			Log.d(TAG, "user clicked reset");
    				    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
			        	alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			        	alertDialog.setTitle(R.string.preference_reset);
			        	alertDialog.setMessage(R.string.preference_reset_question);
			        	alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			        		@Override
					        public void onClick(DialogInterface dialog, int which) {
		                		if( MyDebug.LOG )
		                			Log.d(TAG, "user confirmed reset");
		                		SharedPreferences.Editor editor = sharedPreferences.edit();
		                		editor.clear();
		                		editor.putBoolean(PreferenceKeys.FirstTimePreferenceKey, true);
								try {
									PackageInfo pInfo = MyPreferenceFragment.this.getActivity().getPackageManager().getPackageInfo(MyPreferenceFragment.this.getActivity().getPackageName(), 0);
			                        int version_code = pInfo.versionCode;
									editor.putInt(PreferenceKeys.LatestVersionPreferenceKey, version_code);
								}
								catch(NameNotFoundException e) {
									if (MyDebug.LOG)
										Log.d(TAG, "NameNotFoundException exception trying to get version number");
									e.printStackTrace();
								}
		                		editor.apply();
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								main_activity.setDeviceDefaults();
		                		if( MyDebug.LOG )
		                			Log.d(TAG, "user clicked reset - need to restart");
		                		restartOnePlusCamera();
					        }
			        	});
			        	alertDialog.setNegativeButton(android.R.string.no, null);
						final AlertDialog alert = alertDialog.create();
						// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
						alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface arg0) {
								if( MyDebug.LOG )
									Log.d(TAG, "reset dialog dismissed");
								dialogs.remove(alert);
							}
						});
						alert.show();
						dialogs.add(alert);
                	}
                	return false;
                }
            });
        }
	}

	private void restartOnePlusCamera() {
		if( MyDebug.LOG )
			Log.d(TAG, "restartOnePlusCamera");
		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
		main_activity.waitUntilImageQueueEmpty();

		Intent i = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage( getActivity().getBaseContext().getPackageName() );
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}

	public static class SaveFolderChooserDialog extends FolderChooserDialog {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if( MyDebug.LOG )
				Log.d(TAG, "FolderChooserDialog dismissed");
			MainActivity main_activity = (MainActivity)this.getActivity();
			if( main_activity != null ) {
				String new_save_location = this.getChosenFolder();
				main_activity.updateSaveFolder(new_save_location);
			}
			super.onDismiss(dialog);
		}
	}

	private void readFromBundle(String [] values, String [] entries, String preference_key, String default_value, String preference_category_key) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "readFromBundle");
		}
		if( values != null && values.length > 0 ) {
			if( MyDebug.LOG ) {
				Log.d(TAG, "values:");
				for(String value : values) {
					Log.d(TAG, value);
				}
			}
			ListPreference lp = (ListPreference)findPreference(preference_key);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			String value = sharedPreferences.getString(preference_key, default_value);
			if( MyDebug.LOG )
				Log.d(TAG, "    value: " + Arrays.toString(values));
			lp.setValue(value);
		}
		else {
			if( MyDebug.LOG )
				Log.d(TAG, "remove preference " + preference_key + " from category " + preference_category_key);
			Preference pref = findPreference(preference_key);
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference(preference_category_key);
        	pg.removePreference(pref);
		}
	}
	
	public void onResume() {
		super.onResume();
				TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {
			    android.R.attr.colorBackground
		});
		int backgroundColor = array.getColor(0, Color.BLACK);

		getView().setBackgroundColor(backgroundColor);
		array.recycle();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void onPause() {
		super.onPause();
	}

    @Override
    public void onDestroy() {
		if( MyDebug.LOG )
			Log.d(TAG, "onDestroy");
		super.onDestroy();


		for(AlertDialog dialog : dialogs) {
			if( MyDebug.LOG )
				Log.d(TAG, "dismiss dialog: " + dialog);
			dialog.dismiss();
		}

	    Fragment folder_fragment = getFragmentManager().findFragmentByTag("FOLDER_FRAGMENT");
    	if( folder_fragment != null ) {
	        DialogFragment dialogFragment = (DialogFragment)folder_fragment;
			if( MyDebug.LOG )
				Log.d(TAG, "dismiss dialogFragment: " + dialogFragment);
	        dialogFragment.dismiss();
    	}
	}



	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if( MyDebug.LOG )
			Log.d(TAG, "onSharedPreferenceChanged");
	    Preference pref = findPreference(key);
	    if( pref instanceof TwoStatePreference ) {
	    	TwoStatePreference twoStatePref = (TwoStatePreference)pref;
	    	twoStatePref.setChecked(prefs.getBoolean(key, true));
	    }
	    else if( pref instanceof  ListPreference ) {
	    	ListPreference listPref = (ListPreference)pref;
	    	listPref.setValue(prefs.getString(key, ""));
		}
	    setSummary(key);
	}

	/** Programmatically sets summaries as required.
	 *  Remember to call setSummary() from the constructor for any keys we set, to initialise the
	 *  summary.
	 */
	private void setSummary(String key) {
		Preference pref = findPreference(key);
	    if( pref instanceof EditTextPreference ) {
	    	// %s only supported for ListPreference
			// we also display the usual summary if no preference value is set
	    	if( pref.getKey().equals("preference_exif_artist") || pref.getKey().equals("preference_exif_copyright") ) {
				EditTextPreference editTextPref = (EditTextPreference)pref;
				if( editTextPref.getText().length() > 0 ) {
					pref.setSummary(editTextPref.getText());
				}
				else if( pref.getKey().equals("preference_exif_artist") ) {
					pref.setSummary(R.string.preference_exif_artist_summary);
				}
				else if( pref.getKey().equals("preference_exif_copyright") ) {
					pref.setSummary(R.string.preference_exif_copyright_summary);
				}
			}
		}
	}
}
