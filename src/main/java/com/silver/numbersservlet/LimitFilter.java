package com.silver.numbersservlet;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")

public class LimitFilter implements Filter {
    private int limit = 20; //Maximum number of concurrently accepted requests
    private int count; //Currently accepted number of requests
    private Object lock = new Object();

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            boolean ok;
            synchronized (lock) {
                ok = count++ < limit;
            }
            if (ok) {
                // BAU
                chain.doFilter(request, response);
            } else {
                /*If 20 ongoing requests, then limit and send error.
                * However, if 20 requests have been accepted, then
                * there there is no way to handle any possible end
                * request anymore as these requests are all waiting
                * for an event that will never happen and no connections open up.*/
                response.getWriter().println(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } finally {
            synchronized (lock) {
                count--;
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}