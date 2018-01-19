package ru.bscmsk.fingerlydemo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by izashkalov on 17.01.2018.
 */

public abstract class AbstractBaseActivity extends AppCompatActivity {

	private FrameLayout flContent;
	private LayoutInflater inflater;
	private LinearLayout llProgress;
	private TextView tvProgressText;
	private AlertDialog alertDialog;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_base_layout);
		flContent = (FrameLayout) findViewById(R.id.fl_content);

		llProgress = (LinearLayout) findViewById(R.id.ll_progress);
		tvProgressText = (TextView) findViewById(R.id.tv_progress_text);
		inflater = LayoutInflater.from(this);

		initUI();
		fillUI();
	}

	@Override
	public void setContentView(int layoutResID) {
		inflater.inflate(layoutResID, flContent);
	}

	protected abstract void initUI();
	protected abstract void fillUI();

	protected void showProgress(CharSequence text){
		runOnUiThread(() -> {
			tvProgressText.setText(text);
			llProgress.setVisibility(View.VISIBLE);
		});
	}

	protected void showDialogSuccesButton(CharSequence text, DialogInterface.OnClickListener listener){
		if(alertDialog != null && alertDialog.isShowing())
			return;

		alertDialog = new AlertDialog.Builder(this)
			.setIcon(R.drawable.logo_bsc_basic)
			.setMessage(text)
			.setPositiveButton("OK", listener)
			.create();

		alertDialog.show();
	}

	protected void showDialogSuccesCancelButton(CharSequence text){
		if(alertDialog != null && alertDialog.isShowing())
			return;

		alertDialog = new AlertDialog.Builder(this)
			.setIcon(R.drawable.logo_bsc_basic)
			.setMessage(text)
			.setPositiveButton("OK", (dialogInterface, i) -> {

			})
			.setNegativeButton("CANCEL", (dialogInterface, i) -> {

			})
			.create();
	}

	protected void hideProgress(){
		runOnUiThread(() -> {
			tvProgressText.setText(null);
			llProgress.setVisibility(View.GONE);
		});
	}
}
