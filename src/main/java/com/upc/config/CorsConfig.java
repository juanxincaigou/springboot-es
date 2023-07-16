package com.upc.config;

//全局跨域配置，Filter过滤器方式


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter(){
        //1. 添加 CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //放行哪些原始域
        // springboot 2.4.0 之前使用这个 或直接指定放行的域名http://localhost:8081...
         config.addAllowedOrigin("*");
//        config.addAllowedOriginPattern("*"); // springboot 2.4.0 之后使用这个addAllowedOriginPattern
        // 是否发送 Cookie
        config.setAllowCredentials(true);
        // 放行哪些请求方式
        config.addAllowedMethod("*");
        // 放行哪些原始请求头部信息
        config.addAllowedHeader("*");

        // 暴露哪些头部信息
        config.addExposedHeader("Content-Type");
        config.addExposedHeader( "X-Requested-With");
        config.addExposedHeader("accept");
        config.addExposedHeader("Origin");
        config.addExposedHeader( "Access-Control-Request-Method");
        config.addExposedHeader("Access-Control-Request-Headers");

        //2. 添加映射路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",config);
        //3. 返回新的CorsFilter
        return new CorsFilter(source);
    }
}

//重写WebMvcConfigurer的addCorsMappings 方法
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")//项目中的所有接口都支持跨域
//                .allowedOriginPatterns("*")//所有地址都可以访问，也可以配置具体地址
//                .allowCredentials(true)
//                .allowedMethods("*")//"GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"
//                .maxAge(3600);// 跨域允许时间
//    }
//}
//注意allowedOrigins("*")和allowCredentials(true)为true时候会出现错误
// 需要改成allowedOriginPatterns("*")或者单独指定接口allowedOrigins("http//www.baidu.com")
//@Configuration
//class WebMvcConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedHeaders("Content-Type","X-Requested-With","accept,Origin","Access-Control-Request-Method","Access-Control-Request-Headers","token")
//                .allowedMethods("*")
//                .allowedOrigins("*")
//                .allowCredentials(true);
//    }
//}