package com.example.arian.googlecalendarprojectscheduler;

import com.google.api.client.util.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by arian on 11/5/2016.
 */
public class EventParser
{
      private class MyEvent
      {
            public int month, day, startHour, startMinute, endHour, endMinute, year;

            public MyEvent() {}

            public int getLengthMinutes()
            {
                  return (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
            }

      }

      private class PotentialEvent
      {

      }

      private int MAXIMUM_ALLOTED_MINUTES       = 180;
      private int MINIMUM_ALOTTED_MINUTES       = 60;
      private int BUFFER_MINUTES_BETWEEN_EVENTS = 15;
      private int EARLIEST_HOUR                 = 8;
      private int LATEST_HOUR                   = 20;

      private int PROJECT_TIME_MINUTES = 0;

      private List<MyEvent> eventList;
      private List<String>  outputList;

      public EventParser()
      {
            eventList = new ArrayList<MyEvent>();
            outputList = new ArrayList<String>();
      }

      public List<String> getResults()
      {
            return outputList;
      }

      public void setMaximumMinutes(int max)
      {
            MAXIMUM_ALLOTED_MINUTES = max * 60;
      }

      public void setMinimumMinutes(int min)
      {
            MINIMUM_ALOTTED_MINUTES = min * 60;
      }

      public void setPaddingMinutes(int mins)
      {
            BUFFER_MINUTES_BETWEEN_EVENTS = mins;
      }

      public void setEarliestHour(int hour)
      {
            EARLIEST_HOUR = hour;
      }

      public void setLatestHour(int hour)
      {
            LATEST_HOUR = hour;
      }

      public void setProjectLength(int hours)
      {
            PROJECT_TIME_MINUTES = hours * 60;
      }

      public void addEvent(DateTime startTime, DateTime endTime)
      {
            Calendar startCal, endCal;
            startCal = Calendar.getInstance();
            endCal = Calendar.getInstance();

            startCal.setTimeInMillis(startTime.getValue());
            endCal.setTimeInMillis(endTime.getValue());

            MyEvent event = new MyEvent();
            event.month = startCal.get(Calendar.MONTH);
            event.day = startCal.get(Calendar.DAY_OF_MONTH);
            event.startHour = startCal.get(Calendar.HOUR_OF_DAY);
            event.startMinute = startCal.get(Calendar.MINUTE);
            event.endHour = endCal.get(Calendar.HOUR_OF_DAY);
            event.endMinute = endCal.get(Calendar.MINUTE);
            event.year = startCal.get(Calendar.YEAR);

            eventList.add(event);
      }

      public boolean generateOutput()
      {
            int lastDay = -1;
            int lastHour = EARLIEST_HOUR, lastMinute = 0;
            boolean first = true;
            MyEvent potentialEvent = null;
            for (MyEvent event : eventList) {

                  if (lastDay != event.day) {

                        if (!first && lastHour < LATEST_HOUR) {
                              int available = LATEST_HOUR * 60 - (lastHour * 60 + lastMinute);
                              if (available >= MINIMUM_ALOTTED_MINUTES + BUFFER_MINUTES_BETWEEN_EVENTS) {
                                    potentialEvent = new MyEvent();
                                    potentialEvent.year = event.year;
                                    potentialEvent.month = event.month;
                                    potentialEvent.day = lastDay;
                                    potentialEvent.startHour = lastHour;
                                    potentialEvent.startMinute = lastMinute;

                                    if (available >= MAXIMUM_ALLOTED_MINUTES)
                                          available = MAXIMUM_ALLOTED_MINUTES;

                                    if (potentialEvent.startHour > EARLIEST_HOUR) {
                                          potentialEvent.startMinute += 15;
                                          available -= BUFFER_MINUTES_BETWEEN_EVENTS;
                                          if (potentialEvent.startMinute >= 60) {
                                                potentialEvent.startMinute -= 60;
                                                potentialEvent.startHour++;

                                          }
                                    }

                                    if (available >= PROJECT_TIME_MINUTES)
                                          available = PROJECT_TIME_MINUTES;

                                    potentialEvent.endHour = potentialEvent.startHour;
                                    potentialEvent.endMinute = potentialEvent.startMinute + available;

                                    while (potentialEvent.endMinute >= 60) {
                                          potentialEvent.endMinute -= 60;
                                          potentialEvent.endHour++;
                                    }
                              }
                        }
                        if (potentialEvent != null) {
                              buildTimeString(potentialEvent);
                              PROJECT_TIME_MINUTES -= potentialEvent.getLengthMinutes();
                              if (PROJECT_TIME_MINUTES <= 0)
                                    return true;
                        }

                        potentialEvent = null;
                        lastHour = EARLIEST_HOUR;
                        lastMinute = 0;
                        lastDay = event.day;
                        first = false;
                  }

                  int currentMinutesTotal = event.startHour * 60 + event.startMinute;
                  int lastMinutesTotal = lastHour * 60 + lastMinute;
                  int availableMinutes = currentMinutesTotal - lastMinutesTotal;

                  //WE HAVE TIME!
                  if (availableMinutes >= MINIMUM_ALOTTED_MINUTES + BUFFER_MINUTES_BETWEEN_EVENTS * 2) {

                        int startHour = lastHour;
                        int startMinutes = lastMinute;
                        if (startHour > EARLIEST_HOUR) {
                              startMinutes += BUFFER_MINUTES_BETWEEN_EVENTS;
                              availableMinutes -= BUFFER_MINUTES_BETWEEN_EVENTS;
                        }


                        //rollover if buffer time puts us to next hour
                        if (startMinutes >= 60) {
                              startHour++;
                              startMinutes -= 60;
                        }

                        int endHour = startHour;
                        int endMinutes = startMinutes;

                        if (availableMinutes > MAXIMUM_ALLOTED_MINUTES)
                              availableMinutes = MAXIMUM_ALLOTED_MINUTES;
                        else
                              availableMinutes -= 15;

                        if (PROJECT_TIME_MINUTES <= availableMinutes)
                              availableMinutes = PROJECT_TIME_MINUTES;

                        endMinutes += availableMinutes;
                        while (endMinutes >= 60) {
                              endMinutes -= 60;
                              endHour++;
                        }

                        if (potentialEvent == null) {
                              potentialEvent = new MyEvent();
                              potentialEvent.year = event.year;
                              potentialEvent.month = event.month;
                              potentialEvent.day = event.day;
                              potentialEvent.startHour = startHour;
                              potentialEvent.startMinute = startMinutes;
                              potentialEvent.endMinute = endMinutes;
                              potentialEvent.endHour = endHour;
                        } else if (availableMinutes > potentialEvent.getLengthMinutes()) {
                              potentialEvent = new MyEvent();
                              potentialEvent.year = event.year;
                              potentialEvent.month = event.month;
                              potentialEvent.day = event.day;
                              potentialEvent.startHour = startHour;
                              potentialEvent.startMinute = startMinutes;
                              potentialEvent.endMinute = endMinutes;
                              potentialEvent.endHour = endHour;
                        }
                  }

                  lastHour = event.endHour;
                  lastMinute = event.endMinute;
            }

            if (PROJECT_TIME_MINUTES > 0) {
                  return false;
            }

            return true;
      }

      public void buildTimeString(MyEvent inputEvent)
      {
            //format "2016-11-05T20:00:00-04:00"
            StringBuilder sb = new StringBuilder();
            sb.append(inputEvent.year);
            sb.append("-");
            sb.append(inputEvent.month + 1);
            sb.append("-");
            if (inputEvent.day < 10)
                  sb.append("0");
            sb.append(inputEvent.day);
            sb.append("T");
            if (inputEvent.startHour < 10)
                  sb.append("0");
            sb.append(inputEvent.startHour);
            sb.append(":");
            if (inputEvent.startMinute < 10)
                  sb.append("0");
            sb.append(inputEvent.startMinute);
            sb.append(":00-05:00");

            //THIS DELIMITES THE START AND END DATES
            //YOU MUST SPLIT BY THIS DELIMITER
            sb.append("?");


            sb.append(inputEvent.year);
            sb.append("-");
            sb.append(inputEvent.month + 1);
            sb.append("-");
            if (inputEvent.day < 10)
                  sb.append("0");
            sb.append(inputEvent.day);
            sb.append("T");
            if (inputEvent.endHour < 10)
                  sb.append("0");
            sb.append(inputEvent.endHour);
            sb.append(":");
            if (inputEvent.endMinute < 10)
                  sb.append("0");
            sb.append(inputEvent.endMinute);
            sb.append(":00-05:00");

            outputList.add(sb.toString());
      }
}
