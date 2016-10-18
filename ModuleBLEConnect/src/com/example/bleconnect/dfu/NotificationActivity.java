package com.example.bleconnect.dfu;

import com.example.bleconnect.Main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

// 
public class NotificationActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// If this activity is the root activity of the task, the app is not running
		if (isTaskRoot()) {
			// Start the app before finishing
			final Intent startAppIntent = new Intent(this, Main.class);
			startAppIntent.putExtras(getIntent().getExtras());
			startActivity(startAppIntent);
		}

		// Now finish, which will drop the user in to the activity that was at the top of the task stack
		finish();
	}
}