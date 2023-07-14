/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2020 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.core.logging;

import com.google.common.annotations.VisibleForTesting;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.Utils;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class keeps the last N lines in a buffer
 *
 * @author matt
 */
public class LoggingBuffer {
  private String name = null;

  private final Deque<BufferLine> buffer = new ConcurrentLinkedDeque<>();
  private final AtomicInteger atomicBufferCount=new AtomicInteger();

  private int bufferSize;

  private KettleLogLayout layout;

  private final List<KettleLoggingEventListener> eventListeners = new CopyOnWriteArrayList<>();

  private final LoggingRegistry loggingRegistry = LoggingRegistry.getInstance();

  public LoggingBuffer( int bufferSize ) {
    this.bufferSize = bufferSize;
    layout = new KettleLogLayout( true );
  }

  /**
   * @return the number (sequence, 1..N) of the last log line. If no records are present in the buffer, 0 is returned.
   */
  public int getLastBufferLineNr() {
      BufferLine lasBufferLine = buffer.peekLast();
      return lasBufferLine != null ? lasBufferLine.getNr() : 0;
  }

  /**
   * @param channelId      channel IDs to grab
   * @param includeGeneral include general log lines
   * @param from
   * @param to
   * @return
   */
  public Stream<KettleLoggingEvent> getLogBufferStreamFromTo( List<String> channelId, boolean includeGeneral, int from,
                                                      int to ) {
    Stream<BufferLine> bufferStream = buffer.stream();

    if ( !Utils.isEmpty( channelId ) ) {
      bufferStream = bufferStream.filter( line -> {
        if ( line.getNr() > from && line.getNr() <= to ) {
          String logChannelId = getLogChId( line );
          return channelId.contains( logChannelId ) || ( includeGeneral && isGeneral( logChannelId ) );
        }
        return false;
      } );
    } else {
      bufferStream = bufferStream.filter( line -> line.getNr() > from && line.getNr() <= to );
    }

    return bufferStream.map( BufferLine::getEvent );
  }

  /**
   * @param parentLogChannelId the parent log channel ID to grab
   * @param includeGeneral     include general log lines
   * @param from
   * @param to
   * @return
   */
  public Stream<KettleLoggingEvent> getLogBufferStreamFromTo( String parentLogChannelId, boolean includeGeneral,
                                                              int from, int to ) {

    // Typically, the log channel id is the one from the transformation or job running currently.
    // However, we also want to see the details of the steps etc.
    // So we need to look at the parents all the way up if needed...
    //
    List<String> childIds = loggingRegistry.getLogChannelChildren( parentLogChannelId );

    return getLogBufferStreamFromTo( childIds, includeGeneral, from, to );
  }

  /**
   * @param channelId      channel IDs to grab
   * @param includeGeneral include general log lines
   * @param from
   * @param to
   * @return
   */
  public List<KettleLoggingEvent> getLogBufferFromTo( List<String> channelId, boolean includeGeneral, int from,
                                                      int to ) {

    return getLogBufferStreamFromTo(channelId, includeGeneral, from, to ).collect( Collectors.toList() );
  }

  /**
   * @param parentLogChannelId the parent log channel ID to grab
   * @param includeGeneral     include general log lines
   * @param from
   * @param to
   * @return
   */
  public List<KettleLoggingEvent> getLogBufferFromTo( String parentLogChannelId, boolean includeGeneral, int from,
                                                      int to ) {

    return getLogBufferStreamFromTo( parentLogChannelId, includeGeneral, from, to ).collect( Collectors.toList() );
  }

  public StringBuffer getBuffer( String parentLogChannelId, boolean includeGeneral, int startLineNr, int endLineNr ) {
    return new StringBuffer(
      getLogBufferStreamFromTo( parentLogChannelId, includeGeneral, startLineNr, endLineNr ).map( layout::format )
        .collect( Collectors.joining( Const.CR ) ) ).append( Const.CR );
  }

  public StringBuffer getBuffer( String parentLogChannelId, boolean includeGeneral ) {
    return getBuffer( parentLogChannelId, includeGeneral, 0 );
  }

  public StringBuffer getBuffer( String parentLogChannelId, boolean includeGeneral, int startLineNr ) {
    return getBuffer( parentLogChannelId, includeGeneral, startLineNr, getLastBufferLineNr() );
  }

  public StringBuffer getBuffer() {
    return getBuffer( null, true );
  }

  public void close() {
  }

  public void doAppend( KettleLoggingEvent event ) {
    if ( event.getMessage() instanceof LogMessage ) {
      buffer.add( new BufferLine( event ) );
      if ( atomicBufferCount.incrementAndGet() > bufferSize && bufferSize > 0 ) {
        do {
          buffer.poll();
        } while ( atomicBufferCount.getAndDecrement() > bufferSize && bufferSize > 0 );
      }
    }
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setLayout( KettleLogLayout layout ) {
    this.layout = layout;
  }

  public KettleLogLayout getLayout() {
    return layout;
  }

  public boolean requiresLayout() {
    return true;
  }

  public void clear() {
    buffer.clear();
    atomicBufferCount.set( 0 );
  }

  /**
   * @return the maximum number of lines that this buffer contains, 0 or lower means: no limit
   */
  public int getMaxNrLines() {
    return bufferSize;
  }

  /**
   * @param maxNrLines the maximum number of lines that this buffer should contain, 0 or lower means: no limit
   */
  public void setMaxNrLines( int maxNrLines ) {
    this.bufferSize = maxNrLines;
  }

  /**
   * @return the nrLines
   */
  public int getNrLines() {
    return atomicBufferCount.get();
  }

  /**
   * Removes all rows for the channel with the specified id
   *
   * @param id the id of the logging channel to remove
   */
  public void removeChannelFromBuffer( String id ) {
    buffer.removeIf( line -> {
      if ( id.equals( getLogChId( line ) ) ) {
        atomicBufferCount.decrementAndGet();
        return true;
      }
      return false;
    } );
  }

  public int size() {
    atomicBufferCount.set( buffer.size() );
    return atomicBufferCount.get();
  }

  public void removeGeneralMessages() {
    buffer.removeIf( line -> {
      if ( isGeneral( getLogChId( line ) ) ) {
        atomicBufferCount.decrementAndGet();
        return true;
      }
      return false;
    } );
  }

  /**
   * We should not expose iterator out of the class.
   * Looks like it's only used in tests.
   *
   * Marked deprecated for now.
   * TODO: To be made package-level in future.
   */
  @Deprecated
  @VisibleForTesting
  public Iterator<BufferLine> getBufferIterator() {
    return buffer.iterator();
  }

  /**
   * It looks like this method is not used in the project.
   */
  @Deprecated
  public String dump() {
    StringBuilder buf = new StringBuilder( 50000 );
    buffer.forEach( line -> {
      LogMessage message = (LogMessage) line.getEvent().getMessage();
      buf.append( message.getLogChannelId() ).append( '\t' )
        .append( message.getSubject() ).append( '\n' );
    } );
    return buf.toString();
  }

  /**
   * Was used in a pair with {@link #getBufferLinesBefore(long)}.
   *
   * @deprecated in favor of {@link #removeBufferLinesBefore(long)}.
   */
  @Deprecated
  public void removeBufferLines( List<BufferLine> linesToRemove ) {
    buffer.removeAll( linesToRemove );
    // Force the calculation of the actual size of the buffer
    size();
  }

  /**
   * Was used in a pair with {@link #removeBufferLines(List)}.
   *
   * @deprecated in favor of {@link #removeBufferLinesBefore(long)}.
   */
  @Deprecated
  public List<BufferLine> getBufferLinesBefore( long minTimeBoundary ) {
    return buffer.stream().filter( line -> line.getEvent().timeStamp < minTimeBoundary )
      .collect( Collectors.toList() );
  }

  public void removeBufferLinesBefore( long minTimeBoundary ) {
    buffer.removeIf( line -> {
      if ( line.getEvent().timeStamp < minTimeBoundary ) {
        atomicBufferCount.decrementAndGet();
        return true;
      }
      return false;
    } );
  }

  public void addLogggingEvent( KettleLoggingEvent loggingEvent ) {
    doAppend( loggingEvent );
    eventListeners.forEach( event -> event.eventAdded( loggingEvent ) );
  }

  public void addLoggingEventListener( KettleLoggingEventListener listener ) {
    eventListeners.add( listener );
  }

  public void removeLoggingEventListener( KettleLoggingEventListener listener ) {
    eventListeners.remove( listener );
  }

  private boolean isGeneral( String logChannelId ) {
    LoggingObjectInterface loggingObject = loggingRegistry.getLoggingObject( logChannelId );
    return loggingObject != null && LoggingObjectType.GENERAL.equals( loggingObject.getObjectType() );
  }

  private static String getLogChId( BufferLine bufferLine ) {
    return ( (LogMessage) bufferLine.getEvent().getMessage() ).getLogChannelId();
  }
}
