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

 /**
  * The maximum length that the queue can grow to
  */
 private int maxQueueLength;

 /**
  * The minimum number of threads in this queue's associated thread pool
  */
 private int minThreads;

 /**
  * The maximum number of threads that can be in this queue's associated thread pool
  */
 private int maxThreads;

 /**
  * The current number of threads
  */
 private int currentThreads = 0;


 /**
  * The thread pool that is servicing this request
  */
 private List<HTTPRequestThread> threadPool = new ArrayList<HTTPRequestThread>();

 private boolean running = true;
 
 private Server server;

 /**
  * Creates a new RequestQueue
  */
 public HTTPRequestQueue(int maxQueueLength,
                      int minThreads,
                      int maxThreads,
                      Server server)
 {
     // Initialize our parameters
     this.maxQueueLength = maxQueueLength;
     this.minThreads = minThreads;
     this.maxThreads = maxThreads;
     this.currentThreads = this.minThreads;
     this.server = server;

     // Create the minimum number of threads
     for( int i=0; i<this.minThreads; i++ )
     {
         HTTPRequestThread thread = new HTTPRequestThread( this, i, this.server );
         thread.start();
         this.threadPool.add( thread );
     }
 }

 /**
  * Adds a new object to the end of the queue
  * 
  * @param o     Adds the specified object to the Request Queue
 * @throws Exception 
  */
 public synchronized void add( Socket o ) throws Exception
 {
     // Validate that we have room of the object before we add it to the queue
     if( queue.size() > this.maxQueueLength )
     {
         throw new Exception( "The Request Queue is full. Max size = " + this.maxQueueLength );
     }

     // Add the new object to the end of the queue
     queue.addLast( o );

     // See if we have an available thread to process the request
     boolean availableThread = false;
     for( Iterator i=this.threadPool.iterator(); i.hasNext(); )
     {
         HTTPRequestThread rt = ( HTTPRequestThread )i.next();
         if( !rt.isProcessing() )
         {
             System.out.println( "Found an available thread" );
             availableThread = true;
             break;
         }
         System.out.println( "Thread is busy" );
     }

     // See if we have an available thread
     if( !availableThread )
     {
         if( this.currentThreads < this.maxThreads )
         {
             System.out.println( "Creating a new thread to satisfy the incoming request" );
             HTTPRequestThread thread = new HTTPRequestThread( this, currentThreads++, this.server );
             thread.start();
             this.threadPool.add( thread );
         }
         else
         {
             System.out.println( "Whoops, can't grow the thread pool, guess you have to wait" );
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
             if( !running )
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
     System.out.println( "Shutting down request threads..." );

     // Mark the queue as not running so that we will free up our request threads
     this.running = false;

     // Tell each thread to kill itself
     for( Iterator i=this.threadPool.iterator(); i.hasNext(); )
     {
         HTTPRequestThread rt = ( HTTPRequestThread )i.next();
         rt.killThread();
     }

     // Wake up all threads and let them die
     notifyAll();
 }
}       