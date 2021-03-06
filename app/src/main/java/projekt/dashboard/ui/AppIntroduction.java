package projekt.dashboard.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import projekt.dashboard.R;

/**
 * Created by Nicholas on 2016-03-20.
 */
public class AppIntroduction extends AppIntro {

    // Please DO NOT override onCreate. Use init.
    @Override
    public void init(Bundle savedInstanceState) {

        addSlide(AppIntroFragment.newInstance(getString(R.string.first_slide_title), getString(R.string.first_slide_description), R.drawable.homepage_icon, Color.parseColor("#212021")));
        addSlide(AppIntroFragment.newInstance(getString(R.string.second_slide_title), getString(R.string.second_slide_description), R.drawable.painbrush_palette, Color.parseColor("#212021")));
        addSlide(AppIntroFragment.newInstance(getString(R.string.third_slide_title), getString(R.string.third_slide_description), R.drawable.phone_heart, Color.parseColor("#212021")));
        addSlide(AppIntroFragment.newInstance(getString(R.string.fourth_slide_title), getString(R.string.fourth_slide_description), R.drawable.theme_utilities, Color.parseColor("#212021")));
        addSlide(AppIntroFragment.newInstance(getString(R.string.fifth_slide_title), getString(R.string.fifth_slide_description), R.drawable.needs_root, Color.parseColor("#212021")));
        addSlide(AppIntroFragment.newInstance(getString(R.string.last_slide_title), getString(R.string.last_slide_description), R.drawable.are_you_ready, Color.parseColor("#212021")));

        showDoneButton(true);
        showSkipButton(false);
        setFadeAnimation();
    }

    @Override
    public void onSkipPressed() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putBoolean("first_run", false).commit();
        Intent intent = new Intent(AppIntroduction.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDonePressed() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putBoolean("first_run", false).commit();
        prefs.edit().putBoolean("blacked_out_enabled", false).commit();
        Intent intent = new Intent(AppIntroduction.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onSlideChanged() {
        // Do something when the slide changes.
    }

    @Override
    public void onNextPressed() {
        // Do something when users tap on Next button.
    }

}
