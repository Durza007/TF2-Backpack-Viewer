package com.minder.app.tf2backpack.frontend;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.minder.app.tf2backpack.R;

public class PlayerListActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
