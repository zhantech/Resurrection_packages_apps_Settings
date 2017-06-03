/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.password;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.SetupWizardUtils;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.ChooseLockTypeDialogFragment.OnLockTypeSelectedListener;
import com.android.setupwizardlib.util.WizardManagerHelper;

/**
 * Setup Wizard's version of ChooseLockPassword screen. It inherits the logic and basic structure
 * from ChooseLockPassword class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPassword class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPassword extends ChooseLockPassword {

    public static Intent modifyIntentForSetup(
            Context context,
            Intent chooseLockPasswordIntent) {
        chooseLockPasswordIntent.setClass(context, SetupChooseLockPassword.class);
        chooseLockPasswordIntent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return chooseLockPasswordIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    public static class SetupChooseLockPasswordFragment extends ChooseLockPasswordFragment
            implements OnLockTypeSelectedListener {

        @VisibleForTesting
        static final int REQUEST_SCREEN_LOCK_OPTIONS = 1;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mCancelButton.setText(R.string.skip_label);

            boolean showOptionsButton = getActivity().getIntent().getBooleanExtra(
                    ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, false);
            if (showOptionsButton) {
                Button optionsButton = view.findViewById(R.id.screen_lock_options);
                optionsButton.setVisibility(View.VISIBLE);
                optionsButton.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.screen_lock_options:
                    launchChooseLockGeneric();
                    break;
                case R.id.cancel_button:
                    SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                            getActivity().getIntent()
                                    .getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false));
                    dialog.show(getFragmentManager());
                    break;
                default:
                    super.onClick(v);
            }
        }

        private void launchChooseLockGeneric() {
            ScreenLockType currentLock = mIsAlphaMode
                    ? ScreenLockType.PASSWORD : ScreenLockType.PIN;
            ChooseLockTypeDialogFragment.newInstance(mUserId, currentLock.toString())
                    .show(getChildFragmentManager(), null);
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            // Setup wizard's redaction interstitial is deferred to optional step. Enable that
            // optional step if the lock screen was set up.
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }

        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            Intent activityIntent = getActivity().getIntent();
            Intent intent = new Intent(getContext(), SetupChooseLockGeneric.class);

            // Copy the original extras into the new intent
            if (activityIntent
                    .hasExtra(ChooseLockGenericFragment.EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS)) {
                intent.putExtras(activityIntent.getBundleExtra(
                        ChooseLockGenericFragment.EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS));
            }
            intent.putExtra(LockPatternUtils.PASSWORD_TYPE_KEY, lock.defaultQuality);

            // Propagate the fingerprint challenge
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE,
                    activityIntent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE,
                            false));
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE,
                    activityIntent.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0));

            // The user is already given the choice of the what screen lock to set up. No need to
            // show this button again.
            intent.putExtra(ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, false);

            WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);

            startActivityForResult(intent, REQUEST_SCREEN_LOCK_OPTIONS);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_SCREEN_LOCK_OPTIONS) {
                if (resultCode != Activity.RESULT_CANCELED) {
                    Activity activity = getActivity();
                    activity.setResult(resultCode, data);
                    activity.finish();
                }
            }
        }
    }
}
