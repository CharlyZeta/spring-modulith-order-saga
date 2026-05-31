package com.showcase.ordersystem.infrastructure;

import io.micrometer.context.ContextSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class TracingConfig {

    @Bean
    public TaskDecorator contextPropagatingTaskDecorator() {
        return runnable -> {
            ContextSnapshot snapshot = ContextSnapshot.captureAll();
            return () -> {
                try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                    runnable.run();
                }
            };
        };
    }

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor(TaskDecorator decorator) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("modulith-v-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(decorator);
        return executor;
    }
}
