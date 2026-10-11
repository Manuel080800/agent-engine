package io.agentengine.api.auth.filter;

import io.agentengine.core.auth.service.TokenRefreshService;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RefreshSerializationFilter extends OncePerRequestFilter {
    private final TokenRefreshService service;

    public RefreshSerializationFilter(TokenRefreshService service) {
        this.service = service;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"refresh_token".equals(request.getParameter("grant_type")) || request.getParameter("refresh_token") == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            service.execute(request.getParameter("refresh_token"), () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException ex) {
                    throw new FilterFailure(ex);
                }
            });
        } catch (FilterFailure failure) {
            if (failure.getCause() instanceof IOException ex) {
                throw ex;
            }

            throw (ServletException) failure.getCause();
        }
    }

    private static class FilterFailure extends RuntimeException {
        FilterFailure(Exception cause) {
            super(cause);
        }
    }
}
