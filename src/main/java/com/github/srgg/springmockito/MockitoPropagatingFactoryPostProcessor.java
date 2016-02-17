/*
 * ⁣​
 * spring-mockito
 * ⁣⁣
 * Copyright (C) 2014 - 2016 srgg
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */
package com.github.srgg.springmockito;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs registration of all Mocks & Spies declared within specified contexts to be used in Autowiring
 * and prevents original beans creation.
 */
public class MockitoPropagatingFactoryPostProcessor implements BeanFactoryPostProcessor {
    private final Object[] contexts;
    private boolean includeInjectMocks;

    public MockitoPropagatingFactoryPostProcessor(Object... contexts) {
        this.contexts = contexts;

        for(Object c: contexts) {
            MockitoAnnotations.initMocks(c);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        final Map<Field,Object> fields = new LinkedHashMap();

        for(Object c:contexts){
            for(Field f: FieldUtils.getAllFields(c.getClass())){
                if(f.isAnnotationPresent(Mock.class)
                        || f.isAnnotationPresent(Spy.class)
                        || includeInjectMocks && f.isAnnotationPresent(InjectMocks.class) ){
                    try {
                        if(!f.isAccessible()){
                            f.setAccessible(true);
                        }

                        Object o = f.get(c);

                        fields.put(f,o);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for(final Map.Entry<Field,Object> e: fields.entrySet()){
            final Field f = e.getKey();

            /*
             * To be processed by BeanPostProcessors, value must be an instance of FactoryBean
             */
            final FactoryBean fb = new SimpleHolderFactoryBean(e.getValue());
            beanFactory.registerSingleton(f.getName(), fb);
        }
    }

    public BeanFactoryPostProcessor setIncludeInjectMocks() {
        includeInjectMocks = true;
        return this;
    }

    private static class SimpleHolderFactoryBean implements FactoryBean{
        private final Object value;

        private SimpleHolderFactoryBean(Object value) {
            this.value = value;
        }

        @Override
        public Object getObject(){
            return value;
        }

        @Override
        public Class<?> getObjectType() {
            return getObject().getClass();
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }
}
