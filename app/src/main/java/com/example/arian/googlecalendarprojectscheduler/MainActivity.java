package com.example.arian.googlecalendarprojectscheduler;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks
{

      DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener()
      {
            public void fuckingCollapseIHateAndroidStudio() {}

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                  DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
                  if (SELECTED_DATE.equals("START")) {
                        startDate = Calendar.getInstance();
                        startDate.set(year, monthOfYear, dayOfMonth);
                        buttonStartDate.setText(df.format(startDate.getTime()));
                  } else {
                        endDate = Calendar.getInstance();
                        endDate.set(year, monthOfYear, dayOfMonth);
                        buttonEndDate.setText(df.format(endDate.getTime()));
                  }
            }
      };

      GoogleAccountCredential mCredential;
      ProgressDialog          mProgress;

      static final int REQUEST_ACCOUNT_PICKER          = 1000;
      static final int REQUEST_AUTHORIZATION           = 1001;
      static final int REQUEST_GOOGLE_PLAY_SERVICES    = 1002;
      static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

      private static final String   PREF_ACCOUNT_NAME = "accountName";
      private static final String[] SCOPES            = {CalendarScopes.CALENDAR};

      Calendar startDate = null;
      Calendar endDate   = null;

      AppCompatButton   buttonStartDate;
      AppCompatButton   buttonEndDate;
      AppCompatEditText editProjectLength;
      AppCompatEditText editProjectName;
      AppCompatEditText editMinimumHours;
      AppCompatEditText editMaximumHours;
      AppCompatEditText editPaddingMinutes;
      AppCompatEditText editEarliestHour;
      AppCompatEditText editLatestHour;
      String SELECTED_DATE = null;

      /**
       * Create the main activity.
       *
       * @param savedInstanceState previously saved instance data.
       */
      @Override
      protected void onCreate(Bundle savedInstanceState)
      {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activty_main);
            getSupportActionBar().setElevation(0);

            mProgress = new ProgressDialog(this);
            mProgress.setMessage("Accessing Google Calendar ...");

            buttonStartDate = (AppCompatButton) findViewById(R.id.button_selectStartDate);
            buttonEndDate = (AppCompatButton) findViewById(R.id.button_selectEndDate);
            editProjectLength = (AppCompatEditText) findViewById(R.id.projectLengthText);
            editProjectName = (AppCompatEditText) findViewById(R.id.projectNameText);
            editMinimumHours = (AppCompatEditText) findViewById(R.id.editMinimumHours);
            editMaximumHours = (AppCompatEditText) findViewById(R.id.editMaximumHours);
            editPaddingMinutes = (AppCompatEditText) findViewById(R.id.editPaddedMinutes);
            editEarliestHour = (AppCompatEditText) findViewById(R.id.editEarliestEvent);
            editLatestHour = (AppCompatEditText) findViewById(R.id.editLatestEvent);

            if (buttonStartDate != null)
                  buttonStartDate.setOnClickListener(new View.OnClickListener()
                  {
                        @Override
                        public void onClick(View v)
                        {
                              SELECTED_DATE = "START";
                              buttonStartDate.setEnabled(false);
                              DatePickerFragment newFragment = new DatePickerFragment();
                              newFragment.setListener(listener);
                              newFragment.setCalendar(startDate);
                              newFragment.show(getSupportFragmentManager(), "datePicker");
                              buttonStartDate.setEnabled(true);
                        }
                  });

            if (buttonEndDate != null)
                  buttonEndDate.setOnClickListener(new View.OnClickListener()
                  {
                        @Override
                        public void onClick(View v)
                        {
                              SELECTED_DATE = "END";
                              buttonEndDate.setEnabled(false);
                              DatePickerFragment newFragment = new DatePickerFragment();
                              newFragment.setListener(listener);
                              newFragment.setCalendar(endDate);
                              newFragment.show(getSupportFragmentManager(), "datePicker");
                              buttonEndDate.setEnabled(true);
                        }
                  });

            // Initialize credentials and service object.
            mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
      }

      @Override
      public boolean onCreateOptionsMenu(Menu menu)
      {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_main, menu);

            // return true so that the menu pop up is opened
            return true;
      }

      @Override
      public boolean onOptionsItemSelected(MenuItem item)
      {
            switch (item.getItemId()) {
                  case R.id.action_schedule:
                        item.setEnabled(false);
                        getResultsFromApi();
                        item.setEnabled(true);
                        return true;
                  default:
                        return super.onOptionsItemSelected(item);
            }
      }

      /**
       * Attempt to call the API, after verifying that all the preconditions are
       * satisfied. The preconditions are: Google Play Services installed, an
       * account was selected and the device currently has online access. If any
       * of the preconditions are not satisfied, the app will prompt the user as
       * appropriate.
       */
      private void getResultsFromApi()
      {
            {
                  if (editProjectName.getText().toString().equals(""))
                        Toast.makeText(MainActivity.this, "Please enter a project name", Toast.LENGTH_SHORT).show();
                  else if (startDate == null)
                        Toast.makeText(MainActivity.this, "Please select a start date", Toast.LENGTH_SHORT).show();
                  else if (endDate == null)
                        Toast.makeText(MainActivity.this, "Please select an end date", Toast.LENGTH_SHORT).show();
                  else if (editProjectLength.getText().toString().equals(""))
                        Toast.makeText(MainActivity.this, "Please enter project length", Toast.LENGTH_SHORT).show();
                  else if (endDate.getTimeInMillis() < startDate.getTimeInMillis())
                        Toast.makeText(MainActivity.this, "End date must be later than start date", Toast.LENGTH_SHORT).show();
                  else {

                        if (!isGooglePlayServicesAvailable()) {
                              acquireGooglePlayServices();
                        } else if (mCredential.getSelectedAccountName() == null) {
                              chooseAccount();
                        } else if (!isDeviceOnline()) {
                              Toast.makeText(MainActivity.this, "No network connection available.", Toast.LENGTH_SHORT).show();
                        } else {
                              new MakeRequestTask(mCredential).execute();
                        }
                  }

            }
      }

      /**
       * Attempts to set the account used with the API credentials. If an account
       * name was previously saved it will use that one; otherwise an account
       * picker dialog will be shown to the user. Note that the setting the
       * account to use with the credentials object requires the app to have the
       * GET_ACCOUNTS permission, which is requested here if it is not already
       * present. The AfterPermissionGranted annotation indicates that this
       * function will be rerun automatically whenever the GET_ACCOUNTS permission
       * is granted.
       */
      @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
      private void chooseAccount()
      {
            if (EasyPermissions.hasPermissions(
                    this, Manifest.permission.GET_ACCOUNTS)) {
                  String accountName = getPreferences(Context.MODE_PRIVATE)
                          .getString(PREF_ACCOUNT_NAME, null);
                  if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                  } else {
                        // Start a dialog from which the user can choose an account
                        startActivityForResult(
                                mCredential.newChooseAccountIntent(),
                                REQUEST_ACCOUNT_PICKER);
                  }
            } else {
                  // Request the GET_ACCOUNTS permission via a user dialog
                  EasyPermissions.requestPermissions(
                          this,
                          "This app needs to access your Google account (via Contacts).",
                          REQUEST_PERMISSION_GET_ACCOUNTS,
                          Manifest.permission.GET_ACCOUNTS);
            }
      }

      /**
       * Called when an activity launched here (specifically, AccountPicker
       * and authorization) exits, giving you the requestCode you started it with,
       * the resultCode it returned, and any additional data from it.
       *
       * @param requestCode code indicating which activity result is incoming.
       * @param resultCode  code indicating the result of the incoming
       *                    activity result.
       * @param data        Intent (containing result data) returned by incoming
       *                    activity result.
       */
      @Override
      protected void onActivityResult(
              int requestCode, int resultCode, Intent data)
      {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                  case REQUEST_GOOGLE_PLAY_SERVICES:
                        if (resultCode != RESULT_OK) {
                              Toast.makeText(MainActivity.this, "This app requires Google Play Services. Please install " +
                                      "Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();

                        } else {
                              getResultsFromApi();
                        }
                        break;
                  case REQUEST_ACCOUNT_PICKER:
                        if (resultCode == RESULT_OK && data != null &&
                                data.getExtras() != null) {
                              String accountName =
                                      data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                              if (accountName != null) {
                                    SharedPreferences settings =
                                            getPreferences(Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                                    editor.apply();
                                    mCredential.setSelectedAccountName(accountName);
                                    getResultsFromApi();
                              }
                        }
                        break;
                  case REQUEST_AUTHORIZATION:
                        if (resultCode == RESULT_OK) {
                              getResultsFromApi();
                        }
                        break;
            }
      }

      /**
       * Respond to requests for permissions at runtime for API 23 and above.
       *
       * @param requestCode  The request code passed in
       *                     requestPermissions(android.app.Activity, String, int, String[])
       * @param permissions  The requested permissions. Never null.
       * @param grantResults The grant results for the corresponding permissions
       *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
       */
      @Override
      public void onRequestPermissionsResult(int requestCode,
                                             @NonNull String[] permissions,
                                             @NonNull int[] grantResults)
      {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            EasyPermissions.onRequestPermissionsResult(
                    requestCode, permissions, grantResults, this);
      }

      /**
       * Callback for when a permission is granted using the EasyPermissions
       * library.
       *
       * @param requestCode The request code associated with the requested
       *                    permission
       * @param list        The requested permission list. Never null.
       */
      @Override
      public void onPermissionsGranted(int requestCode, List<String> list)
      {
            // Do nothing.
      }

      /**
       * Callback for when a permission is denied using the EasyPermissions
       * library.
       *
       * @param requestCode The request code associated with the requested
       *                    permission
       * @param list        The requested permission list. Never null.
       */
      @Override
      public void onPermissionsDenied(int requestCode, List<String> list)
      {
            Toast.makeText(MainActivity.this, "Cannot run without permissions.", Toast.LENGTH_SHORT).show();
            // Do nothing.
      }

      /**
       * Checks whether the device currently has a network connection.
       *
       * @return true if the device has a network connection, false otherwise.
       */

      private boolean isDeviceOnline()
      {
            ConnectivityManager connMgr =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
      }

      /**
       * Check that Google Play services APK is installed and up to date.
       *
       * @return true if Google Play Services is available and up to
       * date on this device; false otherwise.
       */
      private boolean isGooglePlayServicesAvailable()
      {
            GoogleApiAvailability apiAvailability =
                    GoogleApiAvailability.getInstance();
            final int connectionStatusCode =
                    apiAvailability.isGooglePlayServicesAvailable(this);
            return connectionStatusCode == ConnectionResult.SUCCESS;
      }

      /**
       * Attempt to resolve a missing, out-of-date, invalid or disabled Google
       * Play Services installation via a user dialog, if possible.
       */
      private void acquireGooglePlayServices()
      {
            GoogleApiAvailability apiAvailability =
                    GoogleApiAvailability.getInstance();
            final int connectionStatusCode =
                    apiAvailability.isGooglePlayServicesAvailable(this);
            if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
                  showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            }
      }


      /**
       * Display an error dialog showing that Google Play Services is missing
       * or out of date.
       *
       * @param connectionStatusCode code describing the presence (or lack of)
       *                             Google Play Services on this device.
       */
      void showGooglePlayServicesAvailabilityErrorDialog(
              final int connectionStatusCode)
      {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog dialog = apiAvailability.getErrorDialog(
                    MainActivity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
      }

      /**
       * An asynchronous task that handles the Google Calendar API call.
       * Placing the API calls in their own task ensures the UI stays responsive.
       */
      private class MakeRequestTask extends AsyncTask<Void, Void, List<String>>
      {
            private com.google.api.services.calendar.Calendar mService   = null;
            private Exception                                 mLastError = null;

            public MakeRequestTask(GoogleAccountCredential credential)
            {
                  HttpTransport transport = AndroidHttp.newCompatibleTransport();
                  JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                  mService = new com.google.api.services.calendar.Calendar.Builder(
                          transport, jsonFactory, credential)
                          .setApplicationName("Google Calendar Project Scheduler")
                          .build();
            }

            /**
             * Background task to call Google Calendar API.
             *
             * @param params no parameters needed for this task.
             */
            @Override
            protected List<String> doInBackground(Void... params)
            {
                  try {
                        return getDataFromApi();
                  } catch (Exception e) {
                        mLastError = e;
                        cancel(true);
                        return null;
                  }
            }

            /**
             * Fetch a list of the next 10 events from the primary calendar.
             *
             * @return List of Strings describing returned events.
             * @throws IOException
             */
            private List<String> getDataFromApi() throws IOException
            {
                  startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH),
                  0, 0, 0);

                  endDate.set(endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH),
                                23, 59, 59);

                  DateTime startTime = new DateTime(startDate.getTime());
                  DateTime endTime = new DateTime(endDate.getTime());

                  List<String> eventStrings = new ArrayList<String>();
                  Events events = mService.events().list("primary")
                          .setTimeMax(endTime)
                          .setTimeMin(startTime)
                          .setOrderBy("startTime")
                          .setSingleEvents(true)
                          .execute();

                  List<Event> items = events.getItems();
                  EventParser parser = new EventParser();

                  int val = Integer.parseInt(editProjectLength.getText().toString());
                  parser.setProjectLength(val);

                  val = Integer.parseInt(editMinimumHours.getText().toString());
                  parser.setMinimumMinutes(val);

                  val = Integer.parseInt(editMaximumHours.getText().toString());
                  parser.setMaximumMinutes(val);

                  val = Integer.parseInt(editPaddingMinutes.getText().toString());
                  parser.setPaddingMinutes(val);

                  val = Integer.parseInt(editEarliestHour.getText().toString());
                  parser.setEarliestHour(val);

                  val = Integer.parseInt(editLatestHour.getText().toString());
                  parser.setLatestHour(val);

                  for (Event event : items) {
                        DateTime start = event.getStart().getDateTime();
                        DateTime end = event.getEnd().getDateTime();
                        parser.addEvent(start, end);
                  }


                  if (parser.generateOutput()) {
                        eventStrings = parser.getResults();
                        Looper.prepare();
                        addCalendarEvents(eventStrings);
                  } else
                        eventStrings = null;

                  return eventStrings;
            }


            @Override
            protected void onPreExecute()
            {
                  mProgress.show();
            }

            @Override
            protected void onPostExecute(final List<String> output)
            {
                  mProgress.hide();
                  if (output == null || output.size() == 0) {
                        Toast.makeText(getApplicationContext(), "Not enough time available :(", Toast.LENGTH_SHORT).show();
                  } else {

                        if(!isFinishing()) {
                              android.support.v7.app.AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
                              dialogBuilder.setTitle("Events successfully added!");
                              dialogBuilder.setMessage("Go to calendar to review?");
                              dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
                              {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                          String startTime = output.get(0);
                                          String[] val = startTime.split("\\?");
                                          DateTime startDateTime = new DateTime(val[0]);

                                          Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                                          builder.appendPath("time");
                                          ContentUris.appendId(builder, startDateTime.getValue());
                                          Intent intent = new Intent(Intent.ACTION_VIEW)
                                                  .setData(builder.build());
                                          startActivity(intent);
                                    }
                              });
                              dialogBuilder.setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {

                                    }
                              });

                              dialogBuilder.create().show();
                        }
                  }
            }

            private void addCalendarEvents(List<String> eventList)
            {
                  String projectName = editProjectName.getText().toString();
                  for (String s : eventList) {
                        Event event = new Event().setSummary(projectName);

                        String[] val = s.split("\\?");

                        DateTime startDateTime = new DateTime(val[0]);
                        EventDateTime start = new EventDateTime()
                                .setDateTime(startDateTime)
                                .setTimeZone("America/New_York");
                        event.setStart(start);

                        DateTime endDateTime = new DateTime(val[1]);
                        EventDateTime end = new EventDateTime()
                                .setDateTime(endDateTime)
                                .setTimeZone("America/New_York");
                        event.setEnd(end);

                        String calendarId = "primary";

                        try {
                              event = mService.events().insert(calendarId, event).execute();
                        } catch (Exception e) {
                              e.printStackTrace();
                        }

                  }

            }

            @Override
            protected void onCancelled()
            {
                  mProgress.hide();
                  if (mLastError != null) {
                        if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                              showGooglePlayServicesAvailabilityErrorDialog(
                                      ((GooglePlayServicesAvailabilityIOException) mLastError)
                                              .getConnectionStatusCode());
                        } else if (mLastError instanceof UserRecoverableAuthIOException) {
                              startActivityForResult(
                                      ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                      MainActivity.REQUEST_AUTHORIZATION);
                        } else {
                              Toast.makeText(getApplicationContext(), "Something went wrong :(", Toast.LENGTH_SHORT).show();
                        }
                  } else {
                        Toast.makeText(getApplicationContext(), "Request cancelled.", Toast.LENGTH_SHORT).show();
                  }
            }
      }
}

