package com.iutms.feux.server;

import com.iutms.feux.kafka.AlertConsumer;
import com.iutms.feux.kafka.AlertReactor;
import com.iutms.feux.service.FeuxSignalisationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener that starts the {@link AlertConsumer} background thread
 * when the Axis WAR is deployed, enabling closed-loop alert reaction.
 *
 * <p>Registered in {@code web.xml} — not via {@code @WebListener} to avoid
 * potential double-registration with the Axis servlet container.
 */
public class AlertConsumerListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumerListener.class);

    private AlertConsumer alertConsumer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("AlertConsumerListener: starting feux-alert-consumer thread");
        FeuxSignalisationServiceImpl service = new FeuxSignalisationServiceImpl();
        AlertReactor reactor = new AlertReactor(service);
        alertConsumer = new AlertConsumer(reactor);

        Thread t = new Thread(alertConsumer, "feux-alert-consumer");
        t.setDaemon(true);
        t.start();
        log.info("AlertConsumerListener: feux-alert-consumer started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (alertConsumer != null) {
            log.info("AlertConsumerListener: stopping feux-alert-consumer");
            alertConsumer.stop();
        }
    }
}
