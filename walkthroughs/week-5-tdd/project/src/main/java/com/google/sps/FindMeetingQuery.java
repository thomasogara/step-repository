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
  private Collection<String> attendees;

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
      return new LinkedList<TimeRange>();
    }

    // Extract the list of attendees from the request (unmodifiable)
    this.attendees = request.getAttendees();
    // Extract the meeting duration from the request
    final long duration = request.getDuration();

    // Sort the events based on their start time.
    final List<Event> sortedEvents = sortEvents(events);
    
    // 
    final List<TimeRange> sortedUnavailableTimeRanges =
        getUnavailableTimeRanges(sortedEvents);

    final List<TimeRange> squashedUnavailableTimeRanges = 
        squashTimeRanges(sortedUnavailableTimeRanges);

    final List<TimeRange> availableTimes = getAvailableTimes(squashedUnavailableTimeRanges, duration);

    return availableTimes;
  }

  /**
   * Sort a list of events in ascending order on the basis of their start times.
   * @param events The Collection of Events to be sorted
   * @return A sorted List of Events
   */
  private List<Event> sortEvents(Collection<Event> events) {
    final List<Event> sortedEvents = new LinkedList<Event>(events);
    Collections.sort(
        sortedEvents,
        (a, b) -> TimeRange.ORDER_BY_START.compare(a.getWhen(), b.getWhen())
    );
    return sortedEvents;
  }

  /**
   * Get all of the unavailable TimeRanges on the day of the request.
   * @param events The list of all events happening on the day of the request
   * @return A List of TimeRanges during which all attendees are unavailable.
   */
  private List<TimeRange> getUnavailableTimeRanges(List<Event> events){
    return events.
        stream()
            /* Filter out any events which have no attendees in common
             * with this meeting request
             */
            .filter(event -> filterNoRelevantAttendees(event))
                /* Extract the TimeRange from the Event.
                 * From this point forward, the other members of the Event
                 * do not influence the decision process.
                 */
                .map(event -> event.getWhen())
                  /* Collect the stream into a List */
                  .collect(Collectors.toList());
  }

  /**
   * Predicate function returning true if an event has at least one attendee
   * in common with the attendees of the meeting request, false otherwise.
   * @param event The event to be tested against the predicate.
   */
  private boolean filterNoRelevantAttendees(Event event) {
    return event.getAttendees()
        .stream()
            /* If any of the attendees of this event appear in the attendee
            list of the meeting request, return true. Otherwise, false. */
            .anyMatch(attendee -> this.attendees.contains(attendee));
  }

  /**
   * Squash a List of TimeRanges together so that all consecutive overlapping
   * and/or nested events are reformatted as a single TimeRange.
   * @param events The list of events to be squashed.
   * @return A List of TimeRanges, where none of the ranges have an
   * intersection with any other.
   */
  private List<TimeRange> squashTimeRanges(List<TimeRange> events) {
    /* The list to be constructed */
    final LinkedList<TimeRange> squashedEvents = new LinkedList();

    final ListIterator<TimeRange> i = events.listIterator();

    /* Iterate over the list of events for as long as there are events yet to
     * be processed.
     */
    while ( i.hasNext() ) {
      // The first event in the current sequence of events
      final TimeRange first = i.next();

      // The last event in the current sequence of events
      TimeRange last = null;

      // Iterate over the TimeRanges, while there is another TimeRange
      // in the list that might be in this sequence.
      while ( i.hasNext() ) {
        // Append the next TimeRange to the current sequence.
        last = i.next();

        // If this TimeRange is in the sequence, continue.
        if (
            first.contains(last)
            || first.overlaps(last)
        ) {
          continue;
        } 
        // If this TimeRange is not in the sequence, backtrack, and remove it from the sequence.
        else {
          // Reverse the cursor to remove this TimeRange from the current sequence.
          i.previous();
          // Set last to point at the last valid TimeRange in the current sequence.
          // Th
          last = i.previous();
          // Advance the cursor to point at this TimeRange, which will be
          // included in the next sequence.
          i.next();
          // Leave the loop, as the end of the sequence has been identified.
          break;
        }
      }

      // A TimeRange representing the current sequence of TimeRanges, squashed into a single TimeRange
      TimeRange squashed = null;

      /*
       * If the sequence has a length greater than 1, then the sequence must 
       * be constructed to span from the start of the first TimeRange in the
       * sequence to the end of the last TimeRange in the sequence.
       */
      if (last != null && !first.equals(last)) {
        squashed = TimeRange.fromStartEnd(
          /* The squashed TimeRange begins as the start of the first TimeRange
           * in the sequence
           */
          first.start(),
          /* The squashed TimeRange finishes at the end of the greater of the
           * end of the first and last TimeRange.
           * Choosing the greater of these two ensures that nested events are
           * handled correctly.
           * For Example:
           *        |----A----|
           *          |--B--| ^
           *                ^
           * In this example, the end of the 'last' event (B) is before
           * the end of the 'first' event (A).
           */
          Math.max(first.end(), last.end()),
          false);
      } 
      /*
       * If the sequence has a length of 1, then the sequence is equal to the
       * first TimeRange in the sequence.
       */
      else {
        squashed = first;
      }

      // Append the current sequence to the list of squashed events
      squashedEvents.add(squashed);
    }

    return squashedEvents;
  }

  /**
   * Get all of the TimeRanges during which all meeting attendees are available.
   * @param squashedUnavailableTimeRanges A List of TimeRanges, during each of
   * which at least one meeting attendee is unavailable. This list MUST be
   * sorted, and all TimeRanges MUST be mutually exclusive.
   * @return A List of TimeRanges during which all meeting attendees are
   * available.
   */
  private List<TimeRange> getAvailableTimes(List<TimeRange> squashedUnavailableTimeRanges, long duration) {
    LinkedList<TimeRange> availableTimes = new LinkedList<TimeRange>();
    int lastUnavailableEndTime = TimeRange.START_OF_DAY;
    for (TimeRange unavailableTimeRange : squashedUnavailableTimeRanges) {
      final TimeRange available = 
          TimeRange.fromStartEnd(
              lastUnavailableEndTime,
              unavailableTimeRange.start(),
              false
          );
      if (available.duration() >= duration) {
        availableTimes.add(available);
      }
      lastUnavailableEndTime = unavailableTimeRange.end();
    }

    if (lastUnavailableEndTime != TimeRange.END_OF_DAY + 1) {
      availableTimes.add(
          TimeRange.fromStartEnd(
              lastUnavailableEndTime,
              TimeRange.END_OF_DAY,
              true
          )
      );
    }

    return availableTimes;
  }
}
