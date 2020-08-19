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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class FindMeetingQuery {
  /**
   * Find all suitable spans of time during which the given meeting request can be fulfilled, with
   * the given list of attendees' daily events taken into consideration.
   *
   * <p><b>A meeting's duration cannot exceed one day's duration.<b>
   *
   * <p>This function runs in O(E + E*A) time, where E is the number of events on the day of the
   * meeting request, and A is the number of TimeRanges for which the mandatory attendees are
   * available. It is important to note that A is a product of the algorithm, and its size cannot be
   * determined ahead of time.
   *
   * @param events  The list of all events scheduled on the day of the meeting request. Not all
   *                events must be associated with an attendee of the meeting.
   * @param request The details of the meeting request. Duration, attendees, and optional attendees
   *                are included here.
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (events == null) {
      throw new IllegalArgumentException("'events' parameter cannot be null");
    }
    if (request == null) {
      throw new IllegalArgumentException("'request' parameter cannot be null");
    }
    if (request.getAttendees() == null) {
      throw new IllegalArgumentException("'request' parameter's set of attendees cannot be null");
    }
    // The duration must not exceed one day. 
    // Disallowing a request that exceeds 1 day's duration is encoded in the test noOptionsForTooLongOfARequest()
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return new LinkedList<>();
    }

    /* Initially, the entire day is available for the scheduling of the request */
    final TimeRange WHOLE_DAY =
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true);

    /* Find all TimeRanges during which the attendees are all available for a meeting */
    final List<TimeRange> availableTimeRanges =
            schedule(events, request, Arrays.asList(WHOLE_DAY), request.getAttendees());

    /* Search through the TimeRanges during which the attendees are all available for a meeting,
     * and identify the TimeRanges within those, during which optional attendees are also available for
     * a meeting, using the same algorithm as before. */
    final List<TimeRange> availableTimeRangesWithOptsConsidered =
            schedule(events, request, availableTimeRanges, request.getOptionalAttendees());

    /* If possible, invite optional attendees. If not, exclude them. */
    if (!availableTimeRangesWithOptsConsidered.isEmpty()) {
      return availableTimeRangesWithOptsConsidered;
    } else {
      return availableTimeRanges;
    }
  }

  /**
   * Schedule a meeting with the provided constraints.
   *
   * <p>This function runs in O(E*A) time, where E is the number of events, and A is the number of
   * available TimeRange's.
   *
   * @param events              All events on the day of the request.
   * @param request             The request made.
   * @param availableTimeRanges The TimeRanges during which the event can be scheduled.
   * @param attendees           The list of attendees to be invited to the event.
   * @return A List of TimeRanges during which the requested meeting can be held, with all provided
   * constraints taken into account.
   */
  private List<TimeRange> schedule(
          Collection<Event> events,
          MeetingRequest request,
          List<TimeRange> availableTimeRanges,
          Collection<String> attendees) {
    /* Create a List of unavailable TimeRanges during which the provided attendees cannot be scheduled for a meeting. */
    final List<TimeRange> unavailableTimeRanges = getUnavailableTimeRanges(events, attendees);

    /* Create a Set of all minutes of the day during which the provided attendees cannot be scheduled for a meeting */
    final HashSet<Integer> unavailableMinutes = getUnavailableMinutes(unavailableTimeRanges);

    final List<TimeRange> splitRanges = new LinkedList<>();

    /*
     * Split all of the available TimeRanges, with the following constraints:
     * - All TimeRanges must be long enough to accommodate the meeting.
     * - All attendees provided must be available for the TimeRange.
     * - All adjacent TimeRanges must be merged into a single TimeRange.
     *
     * This will produce a List of TimeRanges which are suggested as the times for the requested meeting.
     */
    for (TimeRange range : availableTimeRanges) {
      splitRanges.addAll(splitRange(range, unavailableMinutes, request.getDuration()));
    }

    return splitRanges;
  }

  /**
   * Get all minutes of the day during which at least one attendee is unavailable, using the collection of
   * unavailable TimeRanges provided.
   *
   * <p>This function runs in O(M) time, where M is the total number of unavailable minutes across the
   * collection of unavailable TimeRanges provided.
   *
   * @param unavailableTimeRanges A List of TimeRanges during which at least one attendee is
   *     unavailable to attend a meeting.
   * @return A HashSet of integers containing all minutes of the day during which at least one
   *     attendee is unavailable.
   */
  private HashSet<Integer> getUnavailableMinutes(Collection<TimeRange> unavailableTimeRanges) {
    HashSet<Integer> unavailableMinutes = new HashSet<>();
    /*
     * Iterate over the unavailable Events, and create a Set of all
     * unavailable minutes on the day of the meeting. This Set is used to
     * check whether any given minute is available for scheduling a meeting.
     */
    for (TimeRange range : unavailableTimeRanges) {
      for (int i = range.start(); i < range.end(); i++) {
        unavailableMinutes.add(i);
      }
    }

    return unavailableMinutes;
  }

  /**
   * Get all TimeRanges during which at least one attendee is unavailable.
   *
   * <p>This function runs in O(A * N) time, where A is the total number of attendees of
   * the meeting, and N is the total number of attendees across all events of the day.
   *
   * @param events    The events on the day of the request
   * @param attendees The attendees of the meeting.
   * @return A List of TimeRanges during which at least one of the attendees is unavailable.
   */
  private List<TimeRange> getUnavailableTimeRanges(
          Collection<Event> events, Collection<String> attendees) {
    return events.stream()
            /*
             * Filter out all events which do not have any common attendees
             * with this meeting request.
             */
            .filter(event -> anyIntersection(attendees, event.getAttendees()))
            .map(Event::getWhen)
            .collect(Collectors.toList());
  }

  /**
   * Split a given TimeRange into a List of mutually exclusive ranges, which have a length greater
   * than or equal to {@code duration}.
   *
   * @param range              The range to split.
   * @param unavailableMinutes The unavailable minutes of the day.
   * @param duration           The duration of the meeting request.
   * @return A List of TimeRanges during any of which the meeting can be scheduled. All ranges are
   * as long as possible, so that there are no adjacent ranges.
   */
  private List<TimeRange> splitRange(
          TimeRange range, HashSet<Integer> unavailableMinutes, long duration) {
    final LinkedList<TimeRange> availableTimeRanges = new LinkedList<>();
    /* Initialise a counter variable */
    int i = range.start();

    /*
     * Iterate for as long as there still exists another minute of the current
     * day that has not been queried for availability.
     */
    while (i < range.end()) {
      /* The first minute of the current span of available time */
      int first = i;
      /* The last minute of the current span of available time */
      int last = i;

      /*
       * Extend the upper bound of the current span of available time, until it
       * reaches an unavailable minute.
       */
      while (i < range.end() && !unavailableMinutes.contains(last)) {
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
      if (spanLength >= duration) {
        availableTimeRanges.add(TimeRange.fromStartEnd(first, last, false));
      }
    }

    return availableTimeRanges;
  }

  /**
   * Returns true if there are any common elements between two guest lists. Ordering of the
   * parameters does not matter.
   *
   * @param listA A guest list
   * @param listB A guest list
   * @return Whether there is any intersection between the elements of two Collections.
   */
  private boolean anyIntersection(Collection<String> listA, Collection<String> listB) {
    return listA.stream().anyMatch(listB::contains);
  }
}
