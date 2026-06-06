package com.example.appointment.config;

import com.example.appointment.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 🔹 تعطيل CSRF لمسار H2 فقط
                .csrf(AbstractHttpConfigurer::disable)



                .formLogin(form -> form.disable())
                .httpBasic(AbstractHttpConfigurer::disable)

                // 🔥🔥 تعديل مهم جداً — لازم disable وليس sameOrigin
                .headers(headers -> headers.frameOptions().disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // 🔓 public APIs
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()



                        .requestMatchers("/api/admin/**").hasRole("ADMIN")



                        .requestMatchers(HttpMethod.POST, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/services/**").hasAnyRole("ADMIN", "STAFF", "CUSTOMER")


                        .requestMatchers(HttpMethod.POST, "/api/appointments").hasRole("CUSTOMER")

                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/status").hasAnyRole("ADMIN", "STAFF")


                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/cancel").hasAnyRole("CUSTOMER","ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/my").hasAnyRole("ADMIN","CUSTOMER")


                        .requestMatchers("/api/admin/schedules/**").hasRole("ADMIN")
                        .anyRequest().authenticated()

                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
