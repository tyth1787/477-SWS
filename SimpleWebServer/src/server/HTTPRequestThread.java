/*
 * HTTPRequestThread.java
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

import java.net.*;

/**
 * A Request thread handles incoming requests
 */
public class HTTPRequestThread extends Thread
{

    private HTTPRequestQueue queue;
    private boolean running;
    private boolean processing = false;
    private int threadNumber;
    private ConnectionHandler connectionHandler;
    private Server server;

    /**
     * Creates a new Request Thread
     * 
     * @param queue         The queue that we are associated with
     * @param threadNumber  Our thread number
     */
    public HTTPRequestThread( HTTPRequestQueue queue, int threadNumber, Server server )
    {
        this.queue = queue;
        this.threadNumber = threadNumber;
        this.server = server;
    }

    /**
     * Returns true if we are currently processing a request, false otherwise
     */
    public boolean isProcessing()
    {
        return this.processing;
    }

    /**
     * If a thread is waiting, then wake it up and tell it to die
     */
    public void killThread()
    {
        this.running = false;
    }

    /**
     * The thread's main processing loop
     */
    public void run()
    {
        this.running = true;
        while( running )
        {
            try
            {
                // Obtain the next pending socket from the queue; only process requests if 
                // we are still running. The shutdown mechanism will wake up our threads at this
                // point, so our state could have changed to not running here.
                Object o = queue.getNextObject();
                if( running )
                {
                    Socket socket = ( Socket )o;

                    this.connectionHandler = new ConnectionHandler(server, socket);
                    this.processing = true;

                    this.connectionHandler.run();

                    this.processing = false;
                }
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
}