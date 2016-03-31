/*
 * Copyright 2009-2013 Brian Pellin.
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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import com.android.keepass.R;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;

/** Designed to Pop up a progress dialog, run a thread in the background, 
 *  run cleanup in the current thread, close the dialog.  Without blocking 
 *  the current thread.
 *  
 * @author bpellin
 *
 */
public class ProgressTask implements Runnable {
	private RunnableOnFinish mTask;
	private ProgressDialog mPd;
	
	public ProgressTask(Context ctx, RunnableOnFinish task, int messageId) {
		mTask = task;
		Handler mHandler = new Handler();
		
		// Show process dialog
		mPd = new ProgressDialog(ctx);
		mPd.setCanceledOnTouchOutside(false);
		mPd.setTitle(ctx.getText(R.string.progress_title));
		mPd.setMessage(ctx.getText(messageId));

		// Set code to run when this is finished
		mTask.setStatus(new UpdateStatus(ctx, mHandler, mPd));
		mTask.mFinish = new AfterTask(task.mFinish);
	}
	
	public void run() {
		// Show process dialog
		mPd.show();

		// Start Thread to Run task
		new Thread(mTask).start();
	}
	
	private class AfterTask extends OnFinish {
		
		AfterTask(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			super.run();
			
			// Remove the progress dialog
			mHandler.post(new CloseProcessDialog());
			
		}
		
	}
	
	private class CloseProcessDialog implements Runnable {

		public void run() {
			mPd.dismiss();
		}
	}

}
