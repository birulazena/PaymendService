package com.github.birulazena.PaymentService.security.filter;

import com.github.birulazena.PaymentService.exception.InvalidTokenException;
import com.github.birulazena.PaymentService.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String tokenHead = request.getHeader("Authorization");
        String token;

        try {
            if (tokenHead == null || !tokenHead.startsWith("Bearer "))
                throw new InvalidTokenException("Invalid or missing Authorization header");

            token = tokenHead.substring(7);
            if(!jwtService.validateToken(token))
                throw new InvalidTokenException("Invalid token");
        } catch (InvalidTokenException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write(ex.getMessage());
            return;
        }

        Long userId = jwtService.getUserId(token);
        String role = jwtService.getRole(token);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                        null, token,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
        usernamePasswordAuthenticationToken.setDetails(
                Map.of(
                        "userId", userId,
                        "role", role
                )
        );
        SecurityContextHolder.getContext()
                .setAuthentication(usernamePasswordAuthenticationToken);
        filterChain.doFilter(request, response);
    }
}
