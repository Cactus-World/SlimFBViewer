package de.cactus_world.SlimFBViewer.utils;

import android.content.Context;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import android.util.AttributeSet;

/**
 * SlimFBViewer is an Open Source app realized by Cactus World Android Development <android@cactus-world.de> based on SlimFacebook by Leonardo Rignanese <rignanese.leo@gmail.com>
 * GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
 * GITHUB: https://github.com/cactus_world/SlimFBViewer
 */

public class SwitchWithoutBugs extends SwitchPreferenceCompat {
    public SwitchWithoutBugs(Context context) {
        super(context);
    }

    public SwitchWithoutBugs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchWithoutBugs(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);
    }
}