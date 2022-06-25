package ru.azor.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class KafkaService {
    public static final String TOPIC_NAME = "current-weather-city";
    public static final String SERVER = "127.0.0.1:9092";
    public static final String GROUP_NAME = "azor";

    private KafkaService() {
    }

    private static class SingletonHolder {
        public static final KafkaService HOLDER_INSTANCE = new KafkaService();
    }

    public static KafkaService getInstance() {
        return KafkaService.SingletonHolder.HOLDER_INSTANCE;
    }

    public String readConfirmationOfCityNameFromKafka() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVER);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_NAME);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // earliest, none, latest
//        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(properties);
//        consumer.subscribe(Collections.singleton(TOPIC_NAME));
//        String result = "";
//        ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(30000));
//        log.info("Received records " + records.count());
//        for (ConsumerRecord<Integer, String> record : records) {
//            log.info("Key: " + record.key() + ", value: " + record.value() +
//                    ", offset: " + record.offset() + ", topic: " + record.topic() +
//                    ", partitions: " + record.partition());
//            result = record.value();
//        }

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));
        consumer.poll(Duration.ofSeconds(10));
//        consumer.assignment().forEach(System.out::println);
        AtomicLong maxTimestamp = new AtomicLong();
        AtomicReference<ConsumerRecord<Integer, String>> latestRecord = new AtomicReference<>();
        // get the last offsets for each partition
        consumer.endOffsets(consumer.assignment()).forEach((topicPartition, offset) -> {
            // seek to the last offset of each partition
            consumer.seek(topicPartition, (offset==0) ? offset:offset - 1);
            // poll to get the last record in each partition
            consumer.poll(Duration.ofSeconds(10)).forEach(record -> {
                // the latest record in the 'topic' is the one with the highest timestamp
                if (record.timestamp() > maxTimestamp.get()) {
                    maxTimestamp.set(record.timestamp());
                    latestRecord.set(record);
                }
            });
        });
        String result = latestRecord.get().value();
        consumer.close();
        return result;
    }

    public void sendCityNameToKafka(Integer key, String cityName){
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<Integer, String> producer = new KafkaProducer<>(properties);
        ProducerRecord<Integer, String> record = new ProducerRecord<>(TOPIC_NAME, key, cityName);
        producer.send(record, (metadata, exception) -> {
            if (exception == null){
                log.info("Send value: " + record.value() + ", topic: " + metadata.topic() +
                        ", partitions: " + metadata.partition() +
                        ", offsets: " + metadata.offset() + ", time: " + metadata.timestamp()
                        );
            }else {
                log.error("Error producing " + exception);
            }
        });
        producer.flush();
        producer.close();
    }
}
