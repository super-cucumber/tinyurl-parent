package com.vipgp.tinyurl.dubbo.provider.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/1 22:41
 */
@Configuration
@Slf4j
public class SqlSessionFactoryConfig {

    /* mybatis 配置路径 */
    @Value("${mybatis.config-location:mybatis-config.xml}")
    private String MYBATIS_CONFIG;
    /* mybatis mapper resource 路径 */
    @Value("${mybatis.mapper-locations:mapper/*.xml}")
    private String MAPPER_PATH;


    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) throws IOException {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        /** 设置mybatis configuration 扫描路径 */
        sqlSessionFactoryBean.setConfigLocation(new ClassPathResource(MYBATIS_CONFIG));

        /** 添加mapper 扫描路径 */
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + MAPPER_PATH;

        String[] paths = StringUtils.tokenizeToStringArray(packageSearchPath,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Resource[] allMappers = new Resource[0];

        for (int i = 0; i < paths.length; i++) {
            Resource[] mappers = pathMatchingResourcePatternResolver.getResources(paths[i]);
            allMappers = ArrayUtils.addAll(allMappers, mappers);
        }

        sqlSessionFactoryBean.setMapperLocations(allMappers);

        /** 设置datasource */
        sqlSessionFactoryBean.setDataSource(dataSource);

        return sqlSessionFactoryBean;
    }

}
