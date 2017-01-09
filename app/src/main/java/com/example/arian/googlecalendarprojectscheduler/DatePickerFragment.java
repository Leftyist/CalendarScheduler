package com.example.arian.googlecalendarprojectscheduler;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
{

      private DatePickerDialog.OnDateSetListener listener = null;
      private Calendar c;

      public void setListener(DatePickerDialog.OnDateSetListener listenToMe)
      {
            listener = listenToMe;
      }

      public void setCalendar(Calendar cal)
      {
            c = cal;
      }

      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState)
      {
            int year, month, day;
            // Use the current date as the default date in the picker
            if (c == null)
                  c = Calendar.getInstance();

            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);

            onDateSet((DatePicker) getView(), year, month, day);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), listener, year, month, day);
      }

      public void onDateSet(DatePicker view, int year, int month, int day)
      {

      }
};