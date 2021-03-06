package de.vorb.sokrates.cli;

import de.vorb.sokrates.app.SokratesApp;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

public interface InjectableCommand extends Runnable {

    String getBeanName();

    default boolean isWebApplication() {
        return false;
    }

    default boolean closesAutomatically() {
        return true;
    }

    @Override
    default void run() {
        final DefaultListableBeanFactory parentBeanFactory = new DefaultListableBeanFactory();
        parentBeanFactory.registerSingleton(getBeanName(), this);
        final GenericApplicationContext parentContext = new GenericApplicationContext(parentBeanFactory);
        parentContext.refresh();

        final ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(SokratesApp.class)
                .parent(parentContext)
                .web(isWebApplication() ? WebApplicationType.SERVLET : WebApplicationType.NONE)
                .build()
                .run();

        if (closesAutomatically()) {
            applicationContext.close();
        }
    }

}
