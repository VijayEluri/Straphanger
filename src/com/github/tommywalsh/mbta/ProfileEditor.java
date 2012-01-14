// Copyright 2011-12 Tom Walsh
//
// This program is free software released under version 3
// of the GPL.  See file gpl.txt for more information.

package com.github.tommywalsh.mbta;

import android.app.ListActivity;
import android.app.AlertDialog;

import android.os.Bundle;

import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.util.Vector;

// This is an on-screen editor that lets users edit profiles
//   You may pass a profileId in the Intent you use to spawn the activity
//     If you do, that profile is edited
//     If not, a new profile is created
// 
//   This activity can be spawned to request a return value or not
//     If so, and if the user doesn't cancel, then
//       the changed profile will be returned.
public class ProfileEditor extends ListActivity
{

    //////////// ACTIVITY LIFECYCLE //////////////////////

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepEditBuffer();
        prepGUI();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshGUI();
    }
    
    @Override protected void onPause() {
        super.onPause();
        m_helper.suspend();
    }
    /////////////////////////////////////////////////////



    

    ///////////// DATA STORAGE /////////////////////////

    // This handles communication with the database
    private ProfileEditHelper m_helper;

    // We locally cache the items in the profile list...
    private Vector<ProfileEditHelper.Entry> m_items;
    // ... and the profile name
    private String m_profileName;

    // We need to disable/enable the save button occasionally...
    private Button m_saveButton;
    // ... and update the name in the header
    private TextView m_header;

    /////////////////////////////////////////////////////







    //////////////// EDIT OPERATIONS //////////////////////////


    // This is called to initialize a new edit session when
    // the activity starts up
    private void prepEditBuffer() {
        Intent i = getIntent();
        int profileId = i.getIntExtra(getString(R.string.profile_in_intent), ProfileEditHelper.NEW_PROFILE);
        
        m_helper = new ProfileEditHelper(getApplicationContext(), profileId);
        m_helper.clearBuffer();
        if (profileId != ProfileEditHelper.NEW_PROFILE) {
            m_helper.loadBufferFromPersistentStorage();
        }
    }

    // This is called after the user confirms the save dialog
    private void saveEditBuffer(String bufferName)
    {
        m_helper.setBufferName(bufferName);
        m_helper.saveBufferToPersistentStorage();
        m_saveButton.setEnabled(false);
        finish();
    }


    // This will be called after the user has confirmed that (s)he
    // really wants to delete the current profile
    private void deleteProfile()
    {
        m_helper.deletePersistentStorage();

        // Since we've just deleted our profile, there's nothing
        // left for the editor to do.  So, let's end this activity
        finish();
    }

    // This function needs to figure out which busses stop near
    // the given point, and add them all to the current edit buffer
    private void addBussesNearLocation(double lat, double lng) {
        // Find the relevant busses
        Vector<Integer> newDeparturePoints = ProximityProfileGenerator.getProximityProfile(this, lat, lng, 0.25);

        // Add them to the edit buffer
        m_helper.addItemsToBuffer(newDeparturePoints);

        // Our edit buffer is newly-changed, 
        // so let the user save it if desired
        m_saveButton.setEnabled(true);
        
        // Update the rest of the GUI
        refreshGUI();
    }

    ///////////////////////////////////////////////////////////






    ////////////// GUI MANAGEMENT ////////////////////////

    // This is called to hook up the GUI when the activity starts
    private void prepGUI() {
        setContentView(R.layout.editor);
        

        Button addToProfileButton = (Button)findViewById(R.id.add_to_profile);
        addToProfileButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    launchLocationPicker();
                }});
        

        
        m_saveButton = (Button)findViewById(R.id.save_profile);
        m_saveButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    launchSaveDialog();
                }});
        
        Button deleteProfileButton = (Button)findViewById(R.id.delete_profile);
        deleteProfileButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    launchDeleteDialog();
                }});
        
        Button cancelButton = (Button)findViewById(R.id.cancel_profile);
        cancelButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });

        m_header = (TextView)findViewById(R.id.editor_header);

        // although refreshGUI will do this later, we need it done
        // before setListAdapter
        m_items = m_helper.getItemsFromBuffer();

        setListAdapter(new ProfileInfoAdapter());

        refreshGUI();
    }



    // This is called when we detect that the edit buffer may have changed
    private void refreshGUI() {
        m_profileName = m_helper.getBufferName();
        m_header.setText(m_profileName);
        m_items = m_helper.getItemsFromBuffer();
        ((ProfileInfoAdapter)getListAdapter()).notifyDataSetChanged();
    }

    ////////////////////////////////////////////////////////


















    /////////////// SUB-ACTIVITIES and DIALOGS ////////////////

    private static final int s_locationPickerId = 2050;    


    // This launches a map, from which the user can pick a location.
    // If the user does so, we'll get the picked location in the
    // onActivityResult method
    private void launchLocationPicker() {
        startActivityForResult(new Intent(ProfileEditor.this, 
                                          LocationPicker.class), 
                               s_locationPickerId);
    }


    // Gives the user a confirmation dialog, allowing them to change the 
    // profile name, really save, and/or cancel
    private void launchSaveDialog() {
        final EditText tv = new EditText(ProfileEditor.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEditor.this);
        builder.setTitle("Profile Name");
        builder.setCancelable(true);
        
        if (m_profileName.length() == 0) {
            tv.setText("Untitled");
        } else {
            tv.setText(m_profileName);
        }
        builder.setView(tv);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    saveEditBuffer(tv.getText().toString());
                }
            });
        builder.show();
    }

    // This will launch a confirmation dialog before we really
    // delete the current profile
    private void launchDeleteDialog()
    {
        TextView tv = new TextView(ProfileEditor.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEditor.this);
        builder.setCancelable(true);
        builder.setTitle("Delete Profile?");
        tv.setText("For realz???");
        builder.setView(tv);
        builder.setNegativeButton("OMFG! Noooo!", null);
        builder.setPositiveButton("Yes!  GTFO!", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteProfile();
                }
            });
        builder.show();
    }



    // This function will be called when a sub-activity successfully finishes
    // Right now, the only sub-activity we have is the map (location picker)
    @Override public void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            if (request == s_locationPickerId) {

                double lat = data.getDoubleExtra("com.github.tommywalsh.mbta.Lat", 0.0);
                double lng = data.getDoubleExtra("com.github.tommywalsh.mbta.Lng", 0.0);
                addBussesNearLocation(lat, lng);
            }
        }
    }


    /////////////////////////////////////////////////////////////










    // This class fills in the Adapter API to enable the ListView to get
    // filled with m_items.
    private class ProfileInfoAdapter extends BaseAdapter 
    {
        public ProfileInfoAdapter() {
        }

        public int getCount() {
            return m_items.size();
        }
        
        public Object getItem(int position) {
            return m_items.elementAt(position);
        }

        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.profile_entry, null);
            }

            ProfileEditHelper.Entry thisInfo = m_items.elementAt(position);
            
            TextView routeWidget = (TextView) convertView.findViewById(R.id.route_title);
            routeWidget.setText(thisInfo.route);

            TextView subrouteWidget = (TextView) convertView.findViewById(R.id.subroute_title);
            subrouteWidget.setText(thisInfo.subroute);

            TextView stopWidget = (TextView) convertView.findViewById(R.id.stop_title);
            stopWidget.setText(thisInfo.stop);
                
            return convertView;
        }
    }
}

