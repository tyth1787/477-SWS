/*
 * HTTPRequestQueue.java
 * Oct 28, 2012
 *
 * Simple Web Server (SWS) for EE407/507 and CS455/555
 * 
 * Copyright (C) 2011 Chandan Raj Rupakheti, Clarkson University
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 * Contact Us:
 * Chandan Raj Rupakheti (rupakhcr@clarkson.edu)
 * Department of Electrical and Computer Engineering
 * Clarkson University
 * Potsdam
 * NY 13699-5722
 * http://clarkson.edu/~rupakhcr
 */
 
package server;

import java.net.Socket;
import java.util.*;

/**
* A Request Queue accepts new requests and processes them with its associated
* thread pool
*/
public class HTTPRequestQueue
{
 /**
  * Request queue
  */
 private LinkedList<Socket> queue = new LinkedList<Socket>();


 private int maxThreadsAllowedInQueue;
 private int minThreadsRunning;
 private int maxThreadsRunning;
 private int currentThreads = 0;
 private List<HTTPRequestThread> threads = new ArrayList<HTTPRequestThread>();
 private boolean isServerRunning = true;
 private Server server;

 /**
  * Creates a new RequestQueue
  */
 public HTTPRequestQueue(int maxQueueLength,int minThreads,int maxThreads,Server server)
 {
     // Initialize member variables
     this.maxThreadsAllowedInQueue = maxQueueLength;
     this.minThreadsRunning = minThreads;
     this.maxThreadsRunning = maxThreads;
     this.currentThreads = this.minThreadsRunning;
     this.server = server;
     // Create the minimum number of threads that are to be running
     for( int i=0; i<this.minThreadsRunning; i++ )
     {
         HTTPRequestThread thread = new HTTPRequestThread( this, i, this.server );
         thread.start();
         this.threads.add( thread );
     }
 }

 /**
  * Adds a new socket to the end of the queue
  * 
  * @param socket Adds the specified object to the HTTPRequestQueue
 * @throws Exception 
  */
 public synchronized void add( Socket socket ) throws Exception
 {
     // Validate that we have room of the object before we add it to the queue
     if( queue.size() > this.maxThreadsAllowedInQueue )
     {
         throw new Exception( "You have exceeded the maximum number of threads allowed");
     }

     // Add the new object to the end of the queue
     queue.addLast( socket );

     // See if we have an available thread to process the request
     boolean availableThread = false;
     for( Iterator<HTTPRequestThread> i=this.threads.iterator(); i.hasNext(); )
     {
         HTTPRequestThread requestThread = i.next();
         if( !requestThread.isProcessing() )
         {
             availableThread = true;
             break;
         }
     }

     // See if we have an available thread
     if( !availableThread )
     {
         if( this.currentThreads < this.maxThreadsRunning )
         {
             HTTPRequestThread thread = new HTTPRequestThread( this, currentThreads++, this.server );
             thread.start();
             this.threads.add( thread );
         }
     }

     // Wake someone up
     notifyAll();
 }

 /**
  * Returns the first object in the queue
  */
 public synchronized Object getNextObject()
 {
     // Setup waiting on the Request Queue
     while( queue.isEmpty() )
     {
         try
         {
             if( !isServerRunning )
             {
                 // Exit criteria for stopping threads
                 return null;
             }
             wait();
         }
         catch( InterruptedException ie ) {}
     }
     
     // Return the item at the head of the queue
     return queue.removeFirst();
 }

 /**
  * Shuts down the request queue and kills all of the request threads
  */
 public synchronized void shutdown()
 {
     // Mark the queue as not running so that we will free up our request threads
     this.isServerRunning = false;

     // Tell each thread to kill itself
     for( Iterator i=this.threads.iterator(); i.hasNext(); )
     {
         HTTPRequestThread rt = ( HTTPRequestThread )i.next();
         rt.killThread();
     }

     // Wake up all threads and let them die
     notifyAll();
 }
}       