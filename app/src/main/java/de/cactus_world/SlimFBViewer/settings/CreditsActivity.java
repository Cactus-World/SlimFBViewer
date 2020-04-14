package de.cactus_world.SlimFBViewer.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import de.cactus_world.SlimFBViewer.R;

/**
 * SlimFBViewer is an Open Source app realized by Cactus World Android Development <android@cactus-world.de> based on SlimFacebook by Leonardo Rignanese <rignanese.leo@gmail.com>
 * GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
 * GITHUB: https://github.com/cactus_world/SlimFBViewer
 */
public class CreditsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load the fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    //preference fragment
    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.credits);//load the layout
        }
    }
}