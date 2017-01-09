package com.example.arian.googlecalendarprojectscheduler;

import com.google.api.client.util.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by arian on 11/5/2016.
 */
public class EventParser
{
      private class MyEvent
      {
            public int month, dayOfMonth, startHour, startMinute, endHour, endMinute, year;

            public MyEvent() {}

            public MyEvent(Calendar start, MyEvent end)
            {
                  this.month = start.get(Calendar.MONTH);
                  this.dayOfMonth = start.get(Calendar.DAY_OF_MONTH);
                  this.startHour = start.get(Calendar.HOUR);
                  this.startMinute = start.get(Calendar.MINUTE);
                  this.endHour = end.startHour;
                  this.endMinute = end.endMinute;
                  this.year = end.year;

                  this.startMinute += BUFFER_MINUTES_BETWEEN_EVENTS;
                  if (this.startMinute >= 60) {
                        this.startHour++;
                        this.startMinute -= 60;
                  }

                  this.endMinute -= BUFFER_MINUTES_BETWEEN_EVENTS;
                  if (this.endMinute < 0) {
                        this.endHour--;
                        this.endMinute += 60;
                  }
            }

            public int getLengthMinutes()
            {
                  return (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
            }

      }

      private int MAXIMUM_ALLOTED_MINUTES       = 180;
      private int MINIMUM_ALOTTED_MINUTES       = 60;
      private int BUFFER_MINUTES_BETWEEN_EVENTS = 15;
      private int EARLIEST_HOUR                 = 8;
      private int EARLIEST_MINUTE               = 0;
      private int LATEST_HOUR                   = 20;
      private int LATEST_MINUTE                 = 0;
      private Calendar STARTING_DAY;
      private Calendar ENDING_DAY;

      private int PROJECT_TIME_MINUTES = 0;

      private List<MyEvent> eventList;
      private List<String>  outputList;
      private List<MyEvent> intermediateList;

      public EventParser()
      {
            eventList = new ArrayList<MyEvent>();
            intermediateList = new ArrayList<MyEvent>();
            outputList = new ArrayList<String>();
      }

      public List<String> getResults()
      {
            return outputList;
      }

      public void setMaximumMinutes(int hoursMax)
      {
            MAXIMUM_ALLOTED_MINUTES = hoursMax * 60;
      }

      public void setMinimumMinutes(int hoursMin)
      {
            MINIMUM_ALOTTED_MINUTES = hoursMin * 60;
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

      public void setStartingDay(Calendar startDay)
      {
            STARTING_DAY = startDay;
      }

      public void setEndingDay(Calendar endDay)
      {
            ENDING_DAY = endDay;
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
            event.dayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);
            event.startHour = startCal.get(Calendar.HOUR_OF_DAY);
            event.startMinute = startCal.get(Calendar.MINUTE);
            event.endHour = endCal.get(Calendar.HOUR_OF_DAY);
            event.endMinute = endCal.get(Calendar.MINUTE);
            event.year = startCal.get(Calendar.YEAR);

            eventList.add(event);
      }

      private int getDaysBetweenDates(Calendar start, Calendar end)
      {
            int days = 0;
            while (start.before(end)) {
                  days++;
            }
            return days;
      }

      private int getMinutesBetweenTimes(int startHour, int startMinute, int endHour, int endMinute)
      {
            int minutes = 0;
            minutes += (endHour - startHour) * 60;
            minutes += endMinute - startMinute;
            minutes -= BUFFER_MINUTES_BETWEEN_EVENTS * 2;
            return minutes;
      }


      public void findAvailableTimes()
      {
            Calendar checkDay = STARTING_DAY;
            checkDay.set(Calendar.SECOND, 0);
            checkDay.set(Calendar.MILLISECOND, 0);

            ListIterator<MyEvent> eventIterator = eventList.listIterator();
            MyEvent currentEvent;

            if (eventIterator.hasNext())
                  currentEvent = eventIterator.next();
            else
                  currentEvent = null;

            //starting at start date, go through all the days until the end date
            for (int daysBetween = getDaysBetweenDates(STARTING_DAY, ENDING_DAY);
                 daysBetween > 0; daysBetween--, checkDay.add(Calendar.DAY_OF_YEAR, 1)) {

                  //reset hours and minutes on new day
                  checkDay.set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR);
                  checkDay.set(Calendar.MINUTE, EARLIEST_MINUTE);

                  //check if there are existing events on this day
                  while (currentEvent != null && currentEvent.dayOfMonth == checkDay.get(Calendar.DAY_OF_MONTH)) {

                        //time between last event and current event
                        int availableMinutes = getMinutesBetweenTimes(checkDay.get(Calendar.HOUR_OF_DAY),
                                                                      checkDay.get(Calendar.MINUTE),
                                                                      currentEvent.startHour,
                                                                      currentEvent.startMinute);

                        //enough time for a possible event
                        if (availableMinutes > MINIMUM_ALOTTED_MINUTES) {
                              intermediateList.add(new MyEvent(checkDay, currentEvent));
                        }

                        //set checking time to the end of the last event
                        checkDay.set(Calendar.HOUR_OF_DAY, currentEvent.endHour);
                        checkDay.set(Calendar.MINUTE, currentEvent.endMinute);

                        //get the next event
                        if (eventIterator.hasNext())
                              currentEvent = eventIterator.next();
                        else
                              currentEvent = null;
                  }

                  //check the available time between the last event of the day(if any) and the end of the day
                  int availableMinutes = getMinutesBetweenTimes(checkDay.get(Calendar.HOUR_OF_DAY),
                                                                checkDay.get(Calendar.MINUTE),
                                                                LATEST_HOUR, LATEST_MINUTE);

                  if (availableMinutes > MINIMUM_ALOTTED_MINUTES) {
                        intermediateList.add(new MyEvent(checkDay, currentEvent));
                  }
            }
      }

      public void selectTimes()
      {

      }

      public boolean generateOutput()
      {
            int lastDay = -1;
            int lastHour = EARLIEST_HOUR, lastMinute = 0;
            boolean first = true;
            MyEvent potentialEvent = null;
            for (MyEvent event : eventList) {

                  if (lastDay != event.dayOfMonth) {

                        if (!first && lastHour < LATEST_HOUR) {
                              int available = LATEST_HOUR * 60 - (lastHour * 60 + lastMinute);
                              if (available >= MINIMUM_ALOTTED_MINUTES + BUFFER_MINUTES_BETWEEN_EVENTS) {
                                    potentialEvent = new MyEvent();
                                    potentialEvent.year = event.year;
                                    potentialEvent.month = event.month;
                                    potentialEvent.dayOfMonth = lastDay;
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
                        lastDay = event.dayOfMonth;
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
                              potentialEvent.dayOfMonth = event.dayOfMonth;
                              potentialEvent.startHour = startHour;
                              potentialEvent.startMinute = startMinutes;
                              potentialEvent.endMinute = endMinutes;
                              potentialEvent.endHour = endHour;
                        } else if (availableMinutes > potentialEvent.getLengthMinutes()) {
                              potentialEvent = new MyEvent();
                              potentialEvent.year = event.year;
                              potentialEvent.month = event.month;
                              potentialEvent.dayOfMonth = event.dayOfMonth;
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
            if (inputEvent.dayOfMonth < 10)
                  sb.append("0");
            sb.append(inputEvent.dayOfMonth);
            sb.append("T");
            if (inputEvent.startHour < 10)
                  sb.append("0");
            sb.append(inputEvent.startHour);
            sb.append(":");
            if (inputEvent.startMinute < 10)
                  sb.append("0");
            sb.append(inputEvent.startMinute);
            sb.append(":00-05:00");

            //THIS DELIMITS THE START AND END DATES
            //YOU MUST SPLIT BY THIS DELIMITER
            sb.append("?");

            sb.append(inputEvent.year);
            sb.append("-");
            sb.append(inputEvent.month + 1);
            sb.append("-");
            if (inputEvent.dayOfMonth < 10)
                  sb.append("0");
            sb.append(inputEvent.dayOfMonth);
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
