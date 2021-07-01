package com.vipgp.tinyurl.dubbo.provider;

import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;
import com.vipgp.tinyurl.dubbo.provider.dao.DaoBasePackage;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@DubboComponentScan
@SpringBootApplication(exclude = {SpringBootConfiguration.class})
@NacosPropertySource(dataId = "tinyurl-dubbo-provider.properties", autoRefreshed = true)
@MapperScan(basePackageClasses = {DaoBasePackage.class})
public class TinyurlDubboProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(TinyurlDubboProviderApplication.class, args);
	}

}
