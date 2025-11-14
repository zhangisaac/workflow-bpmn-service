package com.example.workflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

// 假设你的 JwtAuthenticationFilter 在同一个包下，若不在需调整导入路径
import com.example.workflow.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // 关键1：注入 JwtAuthenticationFilter（需确保该类已加 @Component 注解）
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 构造函数注入（Spring 会自动查找 @Component 注解的 JwtAuthenticationFilter 实例）
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // 关键2：创建 MvcRequestMatcher.Builder，解决多 Servlet 路径冲突
    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 禁用 CSRF（JWT 认证无需 CSRF）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态会话
                .authorizeHttpRequests(auth -> auth
                        // 1. 放行非 Spring MVC 路径（H2 控制台）
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                        // 2. Actuator endpoints - must be before other patterns
                        .requestMatchers(
                                new AntPathRequestMatcher("/actuator/health"),
                                new AntPathRequestMatcher("/actuator/health/**"),
                                new AntPathRequestMatcher("/actuator/info"),
                                new AntPathRequestMatcher("/actuator/info/**"),
                                mvc.servletPath("/").pattern("/actuator/health"),
                                mvc.servletPath("/").pattern("/actuator/health/**"),
                                mvc.servletPath("/").pattern("/actuator/info"),
                                mvc.servletPath("/").pattern("/actuator/info/**")
                        ).permitAll()
                        // 3. 放行 Spring MVC 路径（显式指定 Servlet 路径为 "/"）
                        // Authentication endpoints - allow all HTTP methods
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/auth/**"),
                                mvc.servletPath("/").pattern("/api/auth/**")
                        ).permitAll()
                        // Swagger UI and OpenAPI documentation - using both MvcRequestMatcher and AntPathRequestMatcher
                        .requestMatchers(
                                new AntPathRequestMatcher("/swagger-ui.html"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/swagger-ui.html/**"),
                                new AntPathRequestMatcher("/swagger-ui/index.html"),
                                mvc.servletPath("/").pattern("/swagger-ui.html"),
                                mvc.servletPath("/").pattern("/swagger-ui/**")
                        ).permitAll()
                        // OpenAPI docs - using both matchers
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/docs"),
                                new AntPathRequestMatcher("/api/docs/**"),
                                new AntPathRequestMatcher("/v3/api-docs"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                mvc.servletPath("/").pattern("/api/docs"),
                                mvc.servletPath("/").pattern("/api/docs/**"),
                                mvc.servletPath("/").pattern("/v3/api-docs"),
                                mvc.servletPath("/").pattern("/v3/api-docs/**")
                        ).permitAll()
                        // 4. 其他所有路径需认证
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // 允许 H2 控制台 iframe 访问
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 添加 JWT 过滤器

        return http.build();
    }

    // 密码加密器（BCrypt 加密）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 认证管理器（用于处理登录认证）
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
