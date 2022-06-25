package ru.azor.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import ru.azor.enums.ErrorValues;

import javax.jms.*;

@Slf4j
public class ActiveMQArtemisService {
    private final static String SET_QUEUE_NAME = "city_name";
    private static final String CONFIRMATION_QUEUE_NAME = "confirmation_city_name";
    private static final String ERROR_QUEUE_NAME = "error_city_name";
    private final static String URL = "tcp://localhost:61616";
    private final static String USERNAME = "azor";
    private final static String PASSWORD = "azor";
    private Session session;
    private MessageConsumer errorConsumer;
    private MessageProducer producer;
    private MessageConsumer consumer;

    private ActiveMQArtemisService() throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(URL, USERNAME, PASSWORD);
        session = null;
        consumer = null;
        errorConsumer = null;
        producer = null;
        Connection connection;
        try{
            connection = connectionFactory.createConnection();
        }catch (Exception exception){
            return;
        }
        assert connection != null;
        connection.start();
        session = connection.createSession(false,
                Session.AUTO_ACKNOWLEDGE);
        Destination setDestination = session.createQueue(SET_QUEUE_NAME);
        Destination errorDestination = session.createQueue(ERROR_QUEUE_NAME);
        Destination confirmationDestination = session.createQueue(CONFIRMATION_QUEUE_NAME);
        consumer = session.createConsumer(confirmationDestination);
        errorConsumer = session.createConsumer(errorDestination);
        producer = session.createProducer(setDestination);
    }

    private static class SingletonHolder {
        public static final ActiveMQArtemisService HOLDER_INSTANCE;

        static {
            try {
                HOLDER_INSTANCE = new ActiveMQArtemisService();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static ActiveMQArtemisService getInstance() {
        return ActiveMQArtemisService.SingletonHolder.HOLDER_INSTANCE;
    }


    public void sendCityNameToArtemis(String cityName) throws Exception {
        if (session == null){
            return;
        }
        TextMessage message =
                session.createTextMessage(cityName);
        producer.send(message);
        log.info("Sent: " + message.getText());
    }

    public String readConfirmationOfCityNameFromArtemis() throws Exception {
        if (consumer == null){
            return ErrorValues.ARTEMIS_FELL.name();
        }
        TextMessage messageReceived = (TextMessage) consumer.receive(3000);
        String response = null;
        if (messageReceived == null) {
            log.error(ErrorValues.NO_SERVICE.name());
            return ErrorValues.NO_SERVICE.name();
        }
        while (messageReceived != null){
            response = messageReceived.getText();
            messageReceived = (TextMessage) consumer.receive(3000);
        }
        log.info("Received message:" + response);
        return response;
    }

    public String readErrorFromActiveMQ() throws JMSException {
        if (errorConsumer == null){
            return "";
        }
        TextMessage messageReceived = (TextMessage) errorConsumer.receive(3000);
        if (messageReceived == null) {
            return "";
        }
        String response = messageReceived.getText();
        log.info("Received error: " + response);
        return response;
    }
}
