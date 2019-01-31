package io.dropwizard.actors;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.actors.config.RMQConfig;
import io.dropwizard.actors.connectivity.RMQConnection;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.Executors;

/**
 * A bundle to add RMQ actors
 */
@Slf4j
public abstract class RabbitmqActorBundle<T extends Configuration> implements ConfiguredBundle<T> {

    @Getter
    private RMQConnection connection;

    private ExecutorServiceProvider executorServiceProvider;

    private MetricRegistry metricRegistry;

    protected RabbitmqActorBundle() {

    }

    protected RabbitmqActorBundle(MetricRegistry metricRegistry, ExecutorServiceProvider executorServiceProvider) {
        this.metricRegistry = metricRegistry;
        this.executorServiceProvider = executorServiceProvider;
    }

    @Override
    public void run(T t, Environment environment) throws Exception {
        val config = getConfig(t);
        val metrics = metrics(environment);
        val executorServiceProvide = executorServiceProvider();
        connection = new RMQConnection(config, metrics,
                executorServiceProvide.newFixedThreadPool("rabbitmq-actors", config.getThreadPoolSize()));
        environment.lifecycle().manage(connection);
        environment.healthChecks().register("rabbitmq-actors", connection.healthcheck());
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    protected abstract RMQConfig getConfig(T t);

    /**
     * Provides metric registry for instrumenting RMQConnection. If method returns null, default metric registry from
     * dropwizard environment is picked
     */

    /**
     * Provides implementation for {@link ExecutorServiceProvider}. Should be overridden if custom executor service
     * implementations needs to be used. For e.g. {@link com.codahale.metrics.InstrumentedExecutorService}.
     */
    protected ExecutorServiceProvider getExecutorServiceProvider(T t) {
        return (name, coreSize) -> Executors.newFixedThreadPool(coreSize);
    }


    private MetricRegistry metrics(Environment environment) {
        if (this.metricRegistry != null) {
            return this.metricRegistry;
        }
        return environment.metrics();
    }

    private ExecutorServiceProvider executorServiceProvider() {
        if (this.executorServiceProvider != null) {
            return executorServiceProvider;
        }
        return (name, coreSize) -> Executors.newFixedThreadPool(coreSize);
    }

}
