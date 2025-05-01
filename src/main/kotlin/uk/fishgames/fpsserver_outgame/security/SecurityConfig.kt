package uk.fishgames.fpsserver_outgame.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtProvider: uk.fishgames.fpsserver_outgame.security.JwtUtil) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**").permitAll()
                it.requestMatchers("/match-wait").permitAll()
                it.requestMatchers("/dedicated/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}

