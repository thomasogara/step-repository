// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Collections;

public final class FindMeetingQuery {
  /**
    * Find all suitable spans of time during which the given meeting request can
    * be fulfilled, with the given list of attendees' daily events being
    * taken into consideration.
    * @param events The list of all events scheduled on the day of the meeting
    * request. Not all events must be associated with an atendee of the meeting.
    * @param request The details of the meeting being scheduled. Duration,
    * attendees, and optional attendees are included here.
    */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // There must be a list of events
    if ( events == null ) {
      throw new IllegalArgumentException("\'events\' parameter cannot be null");
    }
    // There must be a request
    if ( request == null ) {
      throw new IllegalArgumentException("\'request\' parameter cannot be null");
    }
    // There must be a set of attendees
    if ( request.getAttendees() == null ) {
      throw new IllegalArgumentException("\'request\' parameter's set of attendees cannot be null");
    }
    // The duration must not exceed one day
    if ( request.getDuration() > TimeRange.WHOLE_DAY.duration() ) {
      // returning an empty list in the case of a request that exceeds 1 day's
      // duration is encoded in the test noOptionsForTooLongOfARequest()
      return new LinkedList<TimeRange>();
    }

    final List<TimeRange> availableTimeRanges = getAvailableTimeRanges(events, request);

    final List<TimeRange> chosenTimeRanges =
        chooseTimeRanges(events, request, availableTimeRanges);

    return chosenTimeRanges;
  }

  /**
   * Get all available TimeRanges for this meeting request. Optional
   * attendees are not considered.
   * @param events The list of all Events on the day the request.
   * @return A List of TimeRanges which are available to schedule this meeting
   * request.
   */
  private List<TimeRange> getAvailableTimeRanges(Collection<Event> events, MeetingRequest request) {
    /* The list of available TimeRanges to be constructed */
    final LinkedList<TimeRange> availableTimeRanges = new LinkedList();

    /*
     * Create a List of unavailable TimeRanges from the List of Events
     * and the List of Attendees provided.
     */
    final List<Event> unavailableEventsWithoutOptsConsidered =
        events.stream()
            /* 
             * Filter out all events which do not have any common attendees
             * with this meeting request.
             */
            .filter(event -> anyIntersection(request.getAttendees(), event.getAttendees()))
                .collect(Collectors.toList());

    final HashSet<Integer> unavailableMinutes = new HashSet<Integer>();
    
    /*
     * Iterate over the unavailable Events, and create a Set of all
     * unavailable minutes on the day of the meeting. This Set is used to
     * check whether any given minute is available for scheduling a meeting.
     */
    for ( Event event : unavailableEventsWithoutOptsConsidered ) {
      final TimeRange range = event.getWhen();
      for ( int i = range.start(); i < range.end(); i++ ) {
        unavailableMinutes.add(i);
      }
    }
    
    /* Initialise a counter variable */
    int i = TimeRange.START_OF_DAY;

    /*
     * Iterate for as long as there still exists another minute of the current
     * day that has not been queried for availability.
     */
    while ( i <= TimeRange.END_OF_DAY ) {
      /* The first minute of the current span of available time */
      int first = i;
      /* The last minute of the current span of available time */
      int last = i;

      /*
       * Extend the upper bound of the current span of available time, until it
       * reaches an unavailable minute.
       */
      while ( i <= TimeRange.END_OF_DAY && !unavailableMinutes.contains(last) ) {
        i++;
        last = i;
      }

      /*
       * The value of the variable i must be incremented after the discovery of
       * an unavailable minute. If i were not incremented, the program
       * logic would never continue past the first unavailable minute
       * encountered and would enter an infinite loop.
       */
      i++;

      // The length of the discovered span of available time.
      int spanLength = last - first;


      /*
       * If we have found a span of available time whose length is greater than
       * the necessary duration for the meeting requested, then create a
       * TimeRange to represent this available time slot.
       */
      if ( spanLength >= request.getDuration() ) {
        availableTimeRanges.add(
            TimeRange.fromStartEnd(
              first,
              last,
              false
            )
        );
      }
    }

    return availableTimeRanges;
  }

  /**
   * 
   */
  private List<TimeRange> chooseTimeRanges(Collection<Event> events, MeetingRequest request, List<TimeRange> availableTimeRanges) {
    List<TimeRange> unavailableTimeRangesForOpts =
        getUnavailableTimeRangesForOpts(events, request);

    List<TimeRange> availableTimeRangesWithOptsConsidered = 
        availableTimeRanges
            .stream()
                .filter(range -> !schedulingConflict(unavailableTimeRangesForOpts, range))
                    .collect(Collectors.toList());

    if ( !availableTimeRangesWithOptsConsidered.isEmpty() ) {
      return availableTimeRangesWithOptsConsidered;
    } else {
      return availableTimeRanges;
    }
  }

  private List<TimeRange> getUnavailableTimeRangesForOpts(Collection<Event> events, MeetingRequest request) {
    return events
        .stream()
          .filter(
              event -> 
                  anyIntersection(
                      request.getOptionalAttendees(),
                      event.getAttendees()
                  )
          )
              .map(event -> event.getWhen())
                  .collect(Collectors.toList());
  }

  /**
   * Returns true if there are any common elements between two Collections.
   * Ordering of the parameters does not matter.
   * @param eventAttendees The attendees of the event to be tested.
   * @return Whether there is any intersection between the elements of two
   * Collections.
   */
  private boolean anyIntersection(Collection collectionA, Collection collectionB) {
    return collectionA
        .stream()
            .anyMatch(element -> collectionB.contains(element));
  }

  /**
   * Return true if the provided TimeRange is NOT available, according to the
   * Collection of unavailable TimeRanges provided.
   * @param unavailableTimeRanges The Collection of TimeRanges which are 
   * unavailable.
   * @param range The TimeRange to be tested for availability.
   * @return Whether inserting {@code range} into a calendar alongside all
   * the TimeRanges in {@code unavailableTimeRanges} would cause a scheduling
   * conflict.
   */
  private boolean schedulingConflict(Collection<TimeRange> unavailableTimeRanges, TimeRange range) {
    return unavailableTimeRanges
        .stream()
            .anyMatch(unavailableRange -> range.overlaps(unavailableRange));
  }
}
