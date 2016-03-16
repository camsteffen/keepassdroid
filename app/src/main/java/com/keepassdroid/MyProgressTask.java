package com.keepassdroid;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import com.android.keepass.R;

public abstract class MyProgressTask<Params, Result> extends AsyncTask<Params, Void, Result> {

    protected Context context;
    private ProgressDialog progressDialog;
    private int messageID;

    MyProgressTask(Context context, int messageID) {
        this.context = context;
        this.messageID = messageID;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle(context.getText(R.string.progress_title));
        progressDialog.setMessage(context.getString(messageID));
        progressDialog.show();
    }

    @Override
    protected void onProgressUpdate(Void... voids) {
        if(context != null) {
            progressDialog.setMessage(context.getString(this.messageID));
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        progressDialog.dismiss();
    }

    public void updateMessage(int messageID) {
        this.messageID = messageID;
        publishProgress();
    }
}
