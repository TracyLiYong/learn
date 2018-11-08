package com.billow.zuul.security.config;


import com.billow.zuul.security.RestAuthenticationEntryPoint;
import com.billow.zuul.security.auth.login.LoginAuthenticationProcessingFilter;
import com.billow.zuul.security.auth.login.LoginAuthenticationProvider;
import com.billow.zuul.security.auth.token.SkipPathRequestMatcher;
import com.billow.zuul.security.auth.token.TokenAuthenticationProcessingFilter;
import com.billow.zuul.security.auth.token.TokenAuthenticationProvider;
import com.billow.zuul.security.auth.token.extractor.TokenExtractor;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * 配置拦截器的主入口，一启动就加载的
 *
 * @author LiuYongTao
 * @date 2018/5/14 8:58
 */
//@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String TOKEN_HEADER_PARAM = "X-Authorization";
    /**
     * 登陆路径
     */
    private static final String FORM_BASED_LOGIN_ENTRY_POINT = "/api/auth/login";
    private static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
    private static final String TOKEN_BASED_AUTH_ENTRY_POINT2 = "/**/userApi/**";
    private static final String MANAGE_TOKEN_BASED_AUTH_ENTRY_POINT = "/manage/**";
    private static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/refresh_token";

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    // 登陆成功时的处理器
    private final AuthenticationSuccessHandler successHandler;
    // 登陆失败时的处理器
    private final AuthenticationFailureHandler failureHandler;
    // 该类实现了用户登录的逻辑
    private final LoginAuthenticationProvider loginAuthenticationProvider;
    // 该类实现了Token的逻辑
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final TokenExtractor tokenExtractor;

    @Autowired
    public WebSecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint, AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler, LoginAuthenticationProvider loginAuthenticationProvider, TokenAuthenticationProvider tokenAuthenticationProvider, TokenExtractor tokenExtractor) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.tokenExtractor = tokenExtractor;
    }

    /**
     * 构建登陆时的过滤器
     *
     * @return LoginAuthenticationProcessingFilter
     * @author LiuYongTao
     * @date 2018/5/14 9:03
     */
    private LoginAuthenticationProcessingFilter buildLoginProcessingFilter() throws Exception {
        LoginAuthenticationProcessingFilter filter = new LoginAuthenticationProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(super.authenticationManager());
        return filter;
    }

    private TokenAuthenticationProcessingFilter buildTokenAuthenticationProcessingFilter() throws Exception {
        List<String> list = Lists.newArrayList(TOKEN_BASED_AUTH_ENTRY_POINT, MANAGE_TOKEN_BASED_AUTH_ENTRY_POINT,TOKEN_BASED_AUTH_ENTRY_POINT2);
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(list);
        TokenAuthenticationProcessingFilter filter = new TokenAuthenticationProcessingFilter(failureHandler, tokenExtractor, matcher);
        filter.setAuthenticationManager(super.authenticationManager());
        return filter;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        // 配置用户登录过滤器（无序的）
        // （在进入到 AbstractAuthenticationProcessingFilter的实现类时会根据 AuthenticationProvider的实现类中的
        // public boolean supports(Class<?> authentication)的方法返回值决定;
        // 来选择使用那个处理器，如果true(匹配成功)就处理，false(匹配不成功)就使用下一个处理器）
        auth.authenticationProvider(loginAuthenticationProvider);
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable() // 因为使用的是JWT，因此这里可以关闭csrf了
                .exceptionHandling()
                .authenticationEntryPoint(this.authenticationEntryPoint)
                .and()
                // 基于token，所以不需要session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
//                .authorizeRequests()
//                // 允许对于网站无授权访问
//                .antMatchers(FORM_BASED_LOGIN_ENTRY_POINT).permitAll() // Login end-point
//                .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll() // Token refresh end-point
//                .and()
                .authorizeRequests()
                //需要鉴权认证
                .antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected API End-points
                //资源只能被拥有 ADMIN 角色的用户访问
//                .antMatchers(MANAGE_TOKEN_BASED_AUTH_ENTRY_POINT).hasAnyRole(RoleEnum.ADMIN.desc())
                .anyRequest().permitAll()
                .and()
                // 注意添加Filter的先后顺序，会导致先后处理类不同
                .addFilterBefore(buildLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}

