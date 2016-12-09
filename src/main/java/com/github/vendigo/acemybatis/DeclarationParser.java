package com.github.vendigo.acemybatis;

import com.github.vendigo.acemybatis.method.AceMethod;
import com.github.vendigo.acemybatis.method.DelegateMethodImpl;
import com.github.vendigo.acemybatis.method.MethodUtils;
import com.github.vendigo.acemybatis.method.select.ReactiveStreamSelect;
import com.github.vendigo.acemybatis.method.select.SimpleStreamSelect;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.stream.Stream;

public class DeclarationParser {
    private static final Logger log = LoggerFactory.getLogger(DeclarationParser.class);

    public static AceMethod parseMethodDeclaration(Class<?> mapperInterface, SqlSessionFactory sqlSessionFactory,
                                                   Method method) {
        Configuration config = sqlSessionFactory.getConfiguration();
        MapperMethod mapperMethod = new MapperMethod(mapperInterface, method, config);
        MapperMethod.SqlCommand command = new MapperMethod.SqlCommand(config, mapperInterface, method);
        MapperMethod.MethodSignature methodSignature = new MapperMethod.MethodSignature(config, mapperInterface, method);

        switch (command.getType()) {
            case SELECT:
                if (methodSignature.getReturnType().equals(Stream.class)) {
                    if (config.hasStatement(MethodUtils.getCountStatementName(method))) {
                        log.info("Using reactive stream select for {}", method.getName());
                        return new ReactiveStreamSelect(method, methodSignature);
                    } else {
                        log.info("Using simple stream select for {}", method.getName());
                        return new SimpleStreamSelect(method, methodSignature);
                    }
                }
                break;
        }

        log.info("Delegating query for {}", method.getName());
        return new DelegateMethodImpl(mapperMethod);
    }
}
