package ru.azor.services;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Getter
@Setter
@Slf4j
public class RabbitMQService {
    private final static String QUEUE_NAME = "city_name";
    private final static String EXCHANGER_NAME = "city_name_exchanger";
    private final static String HOST_NAME = "localhost";
    private String response;
    private Connection connection;
    private Channel channel;

    private RabbitMQService() {
    }

    private static class SingletonHolder {
        public static final RabbitMQService HOLDER_INSTANCE = new RabbitMQService();
    }

    public static RabbitMQService getInstance() {
        return RabbitMQService.SingletonHolder.HOLDER_INSTANCE;
    }

    public void readConfirmationOfCityNameFromRabbitMQ() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        log.info("Waiting for messages");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            setResponse(new String(delivery.getBody(), StandardCharsets.UTF_8));
            log.info("Response: " + getResponse());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
        });
    }

    public void sendCityNameToRabbitMQ(String cityName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGER_NAME, BuiltinExchangeType.FANOUT, true, false, null);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGER_NAME, "");
            channel.basicPublish(EXCHANGER_NAME, "", null, cityName.getBytes());
            log.info("Sent: " + cityName);
        }
    }
}
