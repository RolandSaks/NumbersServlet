package com.silver.numbersservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
        urlPatterns={"/NumbersServlet", ""},
        name="NumbersServlet",
        loadOnStartup = 1
)

/**
 * Servlet that extends the HttpServlet class to read and respond to specific HTTP POST requests.
 */

public class NumbersServlet extends HttpServlet {

    protected static AtomicLong totalNumber = new AtomicLong(); //Aggregated total to be returned at the end of the cycle
    protected static AtomicBoolean endCycle = new AtomicBoolean(); //Cycle end flag
    private Object lock = new Object(); //Notify lock

    protected void doPost (HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // If this request starts a new cycle, reset values
        if (endCycle.get()) {
            endCycle.set(false);
            totalNumber.set(0);
        }

        String requestData = new String(); //To hold the payload

        //Read the request body
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            requestData = buffer.toString();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }

        //If payload contains digits, then assume a number request
        if (requestData.matches("\\d+$")) {

            //Get current total and add value from request
            totalNumber.getAndAdd(Long.valueOf(requestData).longValue());

            /*Once total is updated for this request, wait for an end request
            * and the event notification*/
            synchronized (lock) {
                while (!endCycle.get()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //When ready, send the aggregated total as response
            response.getWriter().println(totalNumber.toString());

        } else if (requestData.equals("end")) { //End request if "end" is sent

            response.getWriter().println(totalNumber.toString()); //Send total as response

            endCycle.set(true); //Flag the end of the cycle

            synchronized (lock) {
                lock.notifyAll(); //Notify all waiting threads of cycle end
            }

        } else { //If not number nor "end", then no additional processing of this request
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public void init() {
        System.out.println(this.getServletName() + " has started.");
    }

    public void destroy() {
        System.out.println(this.getServletName() + " has stopped.");
    }
}