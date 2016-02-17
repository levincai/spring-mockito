spring-mockito
==============

Significantly simplifies Spring beans mocking with Mockito. MockitoPropagatingFactoryPostProcessor replaces original beans with their mocks, therefore even Autowiring works fine out of the box. You can find Spring Test example below.

Simple step by step tutorial
============================

Let's assume that we have two beans:
   - errorHandler with method onError() which re-throws corresponding  exception,
   - integerConverter with method convert() which is returns integer value parsed from passed string or calls autowired instance of errorHandler in case of error.

Here is a simple implementation of those beans:

```java

    public class ErrorHandler{
        public void onError(Throwable e) throws RuntimeException {
            throw new RuntimeException("Exception was generated upon request", e);
        }
    }

    public class IntegerConverter{
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

```

and corresponding beans.xml:

```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    
        <bean id="errorHandler" class="org.test.ErrorHandler" />
        <bean id="integerConverter" class="org.test.IntegerConverter" />
    </beans>
```


Let's create a test to ensure that errorHandler will be called by integerConverter exactly one time in case of error. So, in terms of Mockito we need to inject the mocked errorHandler and verify interactions count. This might be done by adding MockitoPropagatingFactoryPostProcessor to the Spring context.
MockitoPropagatingFactoryPostProcessor - is a BeanFactoryPostProcessor

```java

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = AutowiredMockTest.MockedConfig.class)
    public class AutowiredMockTest {
        @Configuration
        @ImportResource("classpath:/beans.xml")
        static class MockedConfig{
            @Mock
            private ExceptionGenerator exceptionGenerator;
    
            @Bean
            public ExceptionGenerator exceptionGenerator(){
                return exceptionGenerator;
            }
    
            @Bean
            public MockitoPropagatingFactoryPostProcessor postProcessor(){
                return new MockitoPropagatingFactoryPostProcessor(this);
            }
        }
    
        @Autowired
        private ExceptionGenerator eg;
    
        @Test
        public void mustNotRaiseExceptionIfItsWasMocked() throws Exception {
            assertNotNull("Spring doesn't properly configured", eg);
    
            // original bean always throws exception
            eg.generateException();
    
            verify(eg, Mockito.times(1)).generateException();
            Mockito.verifyNoMoreInteractions(eg);
        }
    }

```

