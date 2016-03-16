/*
 * 
 * Copyright 2009-2015 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import android.app.*;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.NotificationCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.util.Linkify;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.keepass.R;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.exception.SamsungClipboardException;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Util;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class EntryFragment extends Fragment {
	public static final String KEY_ENTRY = "entry";

	private static final int NOTIFY_USERNAME = 1;
	private static final int NOTIFY_PASSWORD = 2;

    private PwEntry mEntry;
	private Timer mTimer = new Timer();
	private boolean mShowPassword;
	private NotificationManager mNM;
	private BroadcastReceiver mIntentReceiver;
	private boolean readOnly = false;
	
	private DateFormat dateFormat;
	private DateFormat timeFormat;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		mShowPassword = ! prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));
		
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.entry_view, container, false);
		
		dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
		timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());

        Database db = ((MainActivity) getActivity()).db;
		readOnly = db.readOnly;

		Bundle args = getArguments();
		UUID uuid = (UUID) args.getSerializable(KEY_ENTRY);
		assert(uuid != null);
		
		mEntry = db.pm.entries.get(uuid);
		
		// Update last access time.
		mEntry.touch(false, false);

        TextView url = (TextView) view.findViewById(R.id.entry_url);
        assert url != null;
        Drawable icon = mEntry.getIcon(db).getDrawable(getContext());
        url.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);

        PwDatabase pm = db.pm;

        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(mEntry.getTitle(true, pm));
        TextView tv6 = (TextView) view.findViewById(R.id.entry_user_name);
        assert tv6 != null;
        tv6.setText(mEntry.getUsername(true, pm));

        url.setText(mEntry.getUrl(true, pm));
        TextView tv4 = (TextView) view.findViewById(R.id.entry_password);
        assert tv4 != null;
        tv4.setText(mEntry.getPassword(true, pm));
        setPasswordStyle(view);

        TextView tv3 = (TextView) view.findViewById(R.id.entry_created);
        assert tv3 != null;
        tv3.setText(getDateTime(mEntry.getCreationTime()));
        TextView tv2 = (TextView) view.findViewById(R.id.entry_modified);
        assert tv2 != null;
        tv2.setText(getDateTime(mEntry.getLastModificationTime()));
        TextView tv1 = (TextView) view.findViewById(R.id.entry_accessed);
        assert tv1 != null;
        tv1.setText(getDateTime(mEntry.getLastAccessTime()));

        Date expires = mEntry.getExpiryTime();
        if ( mEntry.expires() ) {
TextView tv = (TextView) view.findViewById(R.id.entry_expires);
assert tv != null;
tv.setText(getDateTime(expires));
} else {
TextView tv = (TextView) view.findViewById(R.id.entry_expires);
assert tv != null;
tv.setText(R.string.never);
        }
        TextView tv = (TextView) view.findViewById(R.id.entry_comment);
        assert tv != null;
        tv.setText(mEntry.getNotes(true, pm));

        Button edit = (Button) view.findViewById(R.id.entry_edit);
		assert edit != null;
		edit.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				EntryEditActivity.Launch(getActivity(), mEntry);
			}

		});

		if (readOnly) {
			edit.setVisibility(View.GONE);
		}
		
		// Notification Manager
		mNM = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		
		if ( mEntry.getPassword().length() > 0 ) {
			// only show notification if password is available
			Notification password = getNotification(Intents.COPY_PASSWORD, R.string.copy_password);
			mNM.notify(NOTIFY_PASSWORD, password);
		}
		
		if ( mEntry.getUsername().length() > 0 ) {
			// only show notification if username is available
			Notification username = getNotification(Intents.COPY_USERNAME, R.string.copy_username);
			mNM.notify(NOTIFY_USERNAME, username);
		}
			
		mIntentReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if ( action.equals(Intents.COPY_USERNAME) ) {
					String username = mEntry.getUsername();
					if ( username.length() > 0 ) {
						timeoutCopyToClipboard(username);
					}
				} else if ( action.equals(Intents.COPY_PASSWORD) ) {
					String password = mEntry.getPassword();
					if ( password.length() > 0 ) {
						timeoutCopyToClipboard(mEntry.getPassword());
					}
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intents.COPY_USERNAME);
		filter.addAction(Intents.COPY_PASSWORD);
		getContext().registerReceiver(mIntentReceiver, filter);

		return view;
	}
	
	@Override
	public void onDestroy() {
		// These members might never get initialized if the app timed out
		if ( mIntentReceiver != null ) {
			getContext().unregisterReceiver(mIntentReceiver);
		}
		
		if ( mNM != null ) {
			try {
			    mNM.cancelAll();
			} catch (SecurityException e) {
				// Some android devices give a SecurityException when trying to cancel notifications without the WAKE_LOCK permission,
				// we'll ignore these.
			}
		}
		
		super.onDestroy();
	}

	private Notification getNotification(String intentText, int descResId) {
		Intent intent = new Intent(intentText);
		PendingIntent pending = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return new NotificationCompat.Builder(getContext())
				.setContentText(getString(descResId))
				.setSmallIcon(R.drawable.notify)
				.setContentTitle(getString(R.string.app_name))
				.setContentIntent(pending)
				.build();
	}

	private String getDateTime(Date dt) {
		return dateFormat.format(dt) + " " + timeFormat.format(dt);
		
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.entry, menu);
		inflater.inflate(R.menu.main, menu);
		
		MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
		if ( mShowPassword ) {
			togglePassword.setTitle(R.string.menu_hide_password);
		} else {
			togglePassword.setTitle(R.string.menu_showpass);
		}
		
		MenuItem gotoUrl = menu.findItem(R.id.menu_goto_url);
		MenuItem copyUser = menu.findItem(R.id.menu_copy_user);
		MenuItem copyPass = menu.findItem(R.id.menu_copy_pass);
		
		// In API >= 11 onCreateOptionsMenu may be called before onCreate completes
		// so mEntry may not be set
		if (mEntry == null) {
			gotoUrl.setVisible(false);
			copyUser.setVisible(false);
			copyPass.setVisible(false);
		}
		else {
			String url = mEntry.getUrl();
			if (EmptyUtils.isNullOrEmpty(url)) {
				// disable button if url is not available
				gotoUrl.setVisible(false);
			}
			if ( mEntry.getUsername().length() == 0 ) {
				// disable button if username is not available
				copyUser.setVisible(false);
			}
			if ( mEntry.getPassword().length() == 0 ) {
				// disable button if password is not available
				copyPass.setVisible(false);
			}
		}
	}
	
	private void setPasswordStyle(View view) {
		TextView password = (TextView) view.findViewById(R.id.entry_password);
		assert password != null;

		if ( mShowPassword ) {
			password.setTransformationMethod(null);
		} else {
			password.setTransformationMethod(PasswordTransformationMethod.getInstance());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Database db = ((MainActivity) getActivity()).db;
		switch ( item.getItemId() ) {
		case R.id.menu_donate:
			try {
				Util.gotoUrl(getContext(), R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getContext(), R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case R.id.menu_toggle_pass:
			if ( mShowPassword ) {
				item.setTitle(R.string.menu_showpass);
				mShowPassword = false;
			} else {
				item.setTitle(R.string.menu_hide_password);
				mShowPassword = true;
			}
			setPasswordStyle(getView());

			return true;
			
		case R.id.menu_goto_url:
			String url;
			url = mEntry.getUrl();
			
			// Default http:// if no protocol specified
			if ( ! url.contains("://") ) {
				url = "http://" + url;
			}
			
			try {
				Util.gotoUrl(getContext(), url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getContext(), R.string.no_url_handler, Toast.LENGTH_LONG).show();
			}
			return true;
			
		case R.id.menu_copy_user:
			timeoutCopyToClipboard(mEntry.getUsername(true, db.pm));
			return true;
			
		case R.id.menu_copy_pass:
			timeoutCopyToClipboard(mEntry.getPassword(true, db.pm));
			return true;
			
		case R.id.menu_lock:
			getActivity().finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void timeoutCopyToClipboard(String text) {
		try {
			Util.copyToClipboard(getContext(), text);
		} catch (SamsungClipboardException e) {
			showSamsungDialog();
			return;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String sClipClear = prefs.getString(getString(R.string.clipboard_timeout_key), getString(R.string.clipboard_timeout_default));
		
		long clipClearTime = Long.parseLong(sClipClear);
		
		if ( clipClearTime > 0 ) {
			mTimer.schedule(new ClearClipboardTask(getContext(), text), clipClearTime);
		}
	}
	

	// Setup to allow the toast to happen in the foreground
	private final Handler uiThreadCallback = new Handler();

	// Task which clears the clipboard, and sends a toast to the foreground.
	private class ClearClipboardTask extends TimerTask {
		
		private final String mClearText;
		private final Context mCtx;
		
		ClearClipboardTask(Context ctx, String clearText) {
			mClearText = clearText;
			mCtx = ctx;
		}
		
		@Override
		public void run() {
			String currentClip = Util.getClipboard(mCtx);
			
			if ( currentClip.equals(mClearText) ) {
				try {
					Util.copyToClipboard(mCtx, "");
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.ClearClipboard));
				} catch (SamsungClipboardException e) {
					uiThreadCallback.post(new UIToastTask(mCtx, R.string.clipboard_error_clear));
				}
			}
		}
	}
	
	private void showSamsungDialog() {
		String text = getString(R.string.clipboard_error).concat(System.getProperty("line.separator")).concat(getString(R.string.clipboard_error_url));
		SpannableString s = new SpannableString(text);
		TextView tv = new TextView(getContext());
		tv.setText(s);
		tv.setAutoLinkMask(Activity.RESULT_OK);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		Linkify.addLinks(s, Linkify.WEB_URLS);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.clipboard_error_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setView(tv)
			.show();
		
	}
}
