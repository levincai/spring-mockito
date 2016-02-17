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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AutowiredMockTest.MockedConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AutowiredMockTest {

    public static class ErrorHandler{
        public void onError(Throwable e) throws RuntimeException {
            throw new RuntimeException("Exception was generated upon request", e);
        }
    }

    public static class IntegerConverter{
        @Autowired
        private ErrorHandler errorHandler;

        public int convert(String value){
            try {
                return Integer.parseInt(value);
            }catch (NumberFormatException e) {
                errorHandler.onError(e);
                return -1;
            }
        }
    }

    @Configuration
    @ImportResource("classpath:/beans.xml")
    static class MockedConfig{
        @Mock
        private ErrorHandler errorHandler;

        @Bean
        public ErrorHandler exceptionGenerator(){
            return errorHandler;
        }

        @Bean
        public MockitoPropagatingFactoryPostProcessor postProcessor(){
            return new MockitoPropagatingFactoryPostProcessor(this);
        }
    }

    @Autowired
    private ErrorHandler errorHandler;

    @Autowired
    private IntegerConverter integerConverter;

    @Test
    public void shouldCallErrorHandlerExactlyOnce() throws Exception {
        // check that Spring was configured properly
        assertNotNull("Spring doesn't properly configured", integerConverter);
        assertNotNull("Spring doesn't properly configured", errorHandler);

        integerConverter.convert("xYz");

        verify(errorHandler, Mockito.times(1)).onError(any(Throwable.class));
        Mockito.verifyNoMoreInteractions(errorHandler);
    }
}
