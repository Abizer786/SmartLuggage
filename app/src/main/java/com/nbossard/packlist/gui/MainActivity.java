/*
 * PackList is an open-source packing-list for Android
 *
 * Copyright (c) 2017 Nicolas Bossard and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.nbossard.packlist.gui;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nbossard.packlist.PackListApp;
import com.nbossard.packlist.R;
import com.nbossard.packlist.model.Item;
import com.nbossard.packlist.model.ScoredItem;
import com.nbossard.packlist.model.TripItem;
import com.nbossard.packlist.model.Trip;
import com.nbossard.packlist.process.saving.ISaving;
import com.nbossard.packlist.process.saving.ITripChangeListener;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import hotchemi.android.rate.AppRate;

//CHECKSTYLE : BEGIN GENERATED CODE
/*
@startuml
    class com.nbossard.packlist.gui.MainActivity {
    }

    com.nbossard.packlist.gui.ITripListFragmentActivity <|.. com.nbossard.packlist.gui.MainActivity
    com.nbossard.packlist.gui.INewTripFragmentActivity <|.. com.nbossard.packlist.gui.MainActivity
    com.nbossard.packlist.gui.ITripDetailFragmentActivity <|.. com.nbossard.packlist.gui.MainActivity
    com.nbossard.packlist.gui.IMassImportFragmentActivity <|.. com.nbossard.packlist.gui.MainActivity

    com.nbossard.packlist.gui.NewTripFragment <.. com.nbossard.packlist.gui.MainActivity : launch in\n container
    com.nbossard.packlist.gui.TripListFragment <.. com.nbossard.packlist.gui.MainActivity : launch in\n container
    com.nbossard.packlist.gui.AboutActivity <..  com.nbossard.packlist.gui.MainActivity : start through intent
    com.nbossard.packlist.gui.ChangeLogDialog  <..  com.nbossard.packlist.gui.MainActivity
    com.nbossard.packlist.gui.MassImportFragment <..  com.nbossard.packlist.gui.MainActivity : launch in\n container

    ' Moved to main file
    ' ISaving <-- com.nbossard.packlist.gui.MainActivity
    ' com.nbossard.packlist.process.saving.ITripChangeListener <|.. com.nbossard.packlist.gui.MainActivity
@enduml
 */
//CHECKSTYLE : END GENERATED CODE

/**
 * Main activity, supports most fragments.
 */
public class MainActivity
        extends AppCompatActivity
        implements
        ITripListFragmentActivity,
        INewTripFragmentActivity,
        ITripDetailFragmentActivity,
        IItemDetailFragmentActivity,
        IMassImportFragmentActivity,
        ITripChangeListener {

// *********************** CONSTANTS**********************************************************************

    /** Log tag. */
    private static final String TAG = MainActivity.class.getName();

    /**
     * The key used to save currently displayed fragment name in saved instance state.
     */
    private static final String CUR_FRAGMENT_KEY = "CUR_FRAGMENT_KEY";

    // Constants for configuring library : https://github.com/hotchemi/Android-Rate



// *********************** FIELDS ***************************************************************************

    /** The Floating Action Button. */
    private FloatingActionButton mFab;
    private FloatingActionButton mLuggage;

    /** The fragment MainActivity instantiated. */
    private TripListFragment mTripListFragment;

    /** The fragment trip detail if already opened. */
    private TripDetailFragment mTripDetailFragment;

    /**
     * The currently displayed fragment class name.
     */
    private String mCurFragment;

// *********************** iINJECTED FIELDS *****************************************************************

    /** The saving module to retrieve and update data (trips).*/
    @Inject
    protected ISaving mSavingModule;

// *********************** LISTENERS**************************************************************************
    /**
     * Listener on back stack in order to set back title bar when back stack is empty.
     */
    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangeListener = () -> {
        ActionBar supActionBar = getSupportActionBar();
        if (supActionBar != null) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                supActionBar.setTitle(getString(R.string.app_name));
                supActionBar.setDisplayHomeAsUpEnabled(false);
                supActionBar.setHomeButtonEnabled(false);
            } else {
                supActionBar.setDisplayHomeAsUpEnabled(true);
                supActionBar.setHomeButtonEnabled(true);
            }
        } else {
            Log.w(TAG, "onBackStackChanged: can't retrieve action bar, VERY STRANGE");
        }
    };

// *********************** METHODS **************************************************************************

    @Override
    protected final void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Injection boiler plate code
        ((PackListApp) getApplication())
                .getSavingComponent()
                .inject(MainActivity.this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = findViewById(R.id.mainact__fab);
        mLuggage = findViewById(R.id.mainact__fab1);

        getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangeListener);

        // Handle deep-app indexing
        onNewIntent(getIntent());

        // retrieving from saved instance state currently displayed fragment
        if (savedInstanceState != null) {
            Log.d(TAG, "non null savedInstanceState");
            mCurFragment = savedInstanceState.getString(CUR_FRAGMENT_KEY);
            Log.d(TAG, "mCurFragment = " + mCurFragment);
        } else {
            Log.d(TAG, "null savedInstanceState");
        }

        // Moved from onStart, however mSavingModule is null when fragments need it for restore
        mSavingModule.addListener(this);

    }

    @Override
    protected final void onStart() {
        super.onStart();
        // activity is launched first time
        if ((mCurFragment == null)) {
            Log.d(TAG, "onStart() : no previous fragment, displaying default");
            mTripListFragment = openMainActivityFragment();
        } else if (mTripListFragment  == null) {
            // activity is returned
            mTripListFragment = (TripListFragment) getSupportFragmentManager()
                    .findFragmentByTag(TripListFragment.class.getSimpleName());
            Log.d(TAG, "onStart() : found a fragment by tag : " + mTripListFragment);
        }
    }


    @Override
    protected final void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CUR_FRAGMENT_KEY, mCurFragment);
    }

    @Override
    public final void onConfigurationChanged(final Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged() called with: " + "newConfig = [" + newConfig + "]");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Activity destroyed, removes listening for changes on trip change.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mSavingModule.removeListener(this);
    }

    /**
     * Opens the provided fragment in a dialog.
     *
     * @param parDialogStandardFragment fragment to be opened
     */
    private void openDialogFragment(final DialogFragment parDialogStandardFragment) {
        if (parDialogStandardFragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment prev = fm.findFragmentByTag("changelog_dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            parDialogStandardFragment.show(ft, "changelog_dialog");
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


       if (id == android.R.id.home) {
            getSupportFragmentManager().popBackStack();
        }
       else  if (id == R.id.action__about) {
           openAboutActivity();}
       else if (id == R.id.action__settings) {
            openSettingsActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * For deep-app indexing.
     * @param intent sic
     */
    protected final void onNewIntent(final Intent intent) {
        String action = intent.getAction();
        String data = intent.getDataString();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String tripId = data.substring(data.lastIndexOf("/") + 1);
            Trip loadedTrip = mSavingModule.loadSavedTrip(UUID.fromString(tripId));
            openTripDetailFragment(loadedTrip);
        }
    }

    // ----------- implementing interface ITripChangeListener -------------------

    @Override
    public final void onTripChange() {
        mTripListFragment.populateList();

        //update detail trip fragment
        if (mTripDetailFragment != null && mTripDetailFragment.getCurrentTrip() != null) {
            UUID curTripUUID = mTripDetailFragment.getCurrentTrip().getUUID();
            Trip loadedTrip = mSavingModule.loadSavedTrip(curTripUUID);
            mTripDetailFragment.displayTrip(loadedTrip);
        } else {
            Log.w(TAG, "Failed updating TripDetailFragment cause Trip is null");
        }
    }


    // ----------- implementing interface IItemDetailFragmentActivity -------------------

    @Override
    public final void updateItem(final TripItem parItem) {
        boolean resUpdate = mSavingModule.updateItem(parItem);
        if (resUpdate) {
            Log.d(TAG, "updateItem(...) update of item succeeded");
        } else {
            Toast.makeText(this, R.string.toast_update_failed_incompatible_format, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public final Set<String> getListOfCategories() {
        return mSavingModule.getAllCategories();
    }

    @Override
    public final Set<Item> getSetOfItems() {
        return mSavingModule.getAllPossibleItems();
    }

    @Override
    public final List<ScoredItem> getProbableItemsList() {
        return mSavingModule.getProbableItemsList();
    }

    // ----------- implementing interface ITripDetailFragmentActivity -------------------

    @Override
    public final void openMassImportFragment(@Nullable final Trip parTrip) {

        // Create fragment and give it an argument specifying the article it should show
        MassImportFragment newFragment = MassImportFragment.newInstance(parTrip);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(getTargetFragment(), newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurFragment = TripDetailFragment.class.getSimpleName();

        // updating FAB action
        mFab.hide();
        mLuggage.hide();
    }

    /** {@inheritDoc} */
    @Override
    public final void saveTrip(final Trip parTrip) {
        mSavingModule.addOrUpdateTrip(parTrip);

        //update fragments displaying trips
        mTripListFragment.populateList();
        if (mTripDetailFragment != null) {
            mTripDetailFragment.displayTrip(parTrip);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Trip loadSavedTrip(final UUID parTripId)
    {
        return mSavingModule.loadSavedTrip(parTripId);
    }

    // ----------- implementing interface ITripListFragmentActivity -------------------

    /** {@inheritDoc} */
    @Override
    public List<Trip> loadSavedTrips()
    {
        return mSavingModule.loadSavedTrips();
    }

    /** {@inheritDoc} */
    @Override
    public void cloneTrip(final UUID parUUID)
    {
        mSavingModule.cloneTrip(parUUID);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteTrip(final UUID parUUID)
    {
        mSavingModule.deleteTrip(parUUID);
    }

    // ----------- implementing interface IMainActivity -------------------

    /**
     * Handle user click on one line and open a new fragment allowing him to see trip
     * Characteristics.
     * @param parTrip unique
     */
    @Override
    public final TripDetailFragment openTripDetailFragment(final Trip parTrip) {

        Log.d(TAG, "openTripDetailFragment(...) Entering");

        // ensure we are not adding on top of not empty back-stack
        FragmentManager fm = getSupportFragmentManager();
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }

        // Create fragment and give it an argument specifying the article it should show
        mTripDetailFragment =  TripDetailFragment.newInstance(parTrip);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(getTargetFragment(), mTripDetailFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurFragment = TripDetailFragment.class.getSimpleName();

        // No need of updating (hiding) FAB action as this is managed in onAttach

        return mTripDetailFragment;
    }


    @Override
    public final void showFABIfAccurate(final boolean parShow) {
        Log.d(TAG, "showFABIfAccurate() called with: " + "parShow = [" + parShow + "]");

        // It happens that mFab is null when called from fragments and returning from background,
        // under investigation...
        if (mFab != null || mLuggage!=null) {
            FragmentManager fragMgr = getSupportFragmentManager();
            if (parShow && fragMgr.getBackStackEntryCount() == 0) {
                mFab.show();
                mLuggage.show();
            } else {
                mFab.hide();
                mLuggage.hide();
            }
        } else {
            Log.w(TAG, "mFab is null, this is very strange");
        }
    }


    @Override
    public final void updateTitleBar(final String parNewTitleInTitleBar) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(parNewTitleInTitleBar);
        }
    }

    /**
     * Handle user click on "add a trip" button and open a new fragment allowing him to input trip
     * Characteristics.
     * @param parTripUUID unique identifier of Trip
     */
    @Override
    public final void openNewTripFragment(@Nullable final UUID parTripUUID) {

        // Create fragment and give it an argument specifying the article it should show
        NewTripFragment newFragment = NewTripFragment.newInstance(parTripUUID);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(getTargetFragment(), newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurFragment = NewTripFragment.class.getSimpleName();

        // updating FAB action
        mFab.hide();
        mLuggage.hide();
    }
    public final void openLuggageManageFragment(@Nullable final UUID parTripUUID) {


        Log.d(TAG, "openLuggageManageFragment() Entering");

        Intent view = new Intent(this, Luggagemanage.class);
        view.setAction(Intent.ACTION_VIEW);
        startActivity(view);
    }

    @Override
    public final void openItemDetailFragment(final TripItem parItem) {

        // Create fragment and give it an argument specifying the article it should show
        ItemDetailFragment newFragment = ItemDetailFragment.newInstance(parItem);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(getTargetFragment(), newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurFragment = TripDetailFragment.class.getSimpleName();

        // updating FAB action
        mFab.hide();
        mLuggage.hide();
    }

    // ----------- end of implementing interface IMainActivity ------------

// *********************** PRIVATE METHODS ******************************************************************

    private int getTargetFragment() {
        Resources res = getResources();
        int resTarget;
        if (res.getBoolean(R.bool.tablet_layout)) {
            resTarget = R.id.mainactcont__right_fragment;
        } else {
            resTarget =  R.id.mainactcont__fragment;
        }
        return resTarget;
    }

    /** Open {@link AboutActivity} on top of this activity. */
    private void openAboutActivity() {

        Log.d(TAG, "openAboutActivity() Entering");

        Intent view = new Intent(this, AboutActivity.class);
        view.setAction(Intent.ACTION_VIEW);
        startActivity(view);
    }

    /**
     * Open {@link SettingsActivity} on top of this activity.
     */
    private void openSettingsActivity() {

        Log.d(TAG, "openSettingsActivity() Entering");

        Intent view = new Intent(this, SettingsActivity.class);
        view.setAction(Intent.ACTION_VIEW);
        startActivity(view);
    }

    /**
     * Open a new fragment allowing him to view trip list.
     * @return the newly created and displayed TripListFragment
     */
    private TripListFragment openMainActivityFragment() {

        Log.d(TAG, "openMainActivityFragment() Entering");

        // Create fragment and give it an argument specifying the article it should show
        TripListFragment newFragment = new TripListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.mainactcont__fragment, newFragment, newFragment.getClass().getSimpleName());
        // NO add to back stack, this is lowest level fragment

        // Commit the transaction
        transaction.commit();
        mCurFragment = TripDetailFragment.class.getSimpleName();

        // updating FAB action
        mFab.show();
        mLuggage.show();
        mLuggage.setOnClickListener(view -> openLuggageManageFragment(null));
        mFab.setOnClickListener(view -> openNewTripFragment(null));
        return newFragment;
    }
//
}
