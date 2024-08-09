package untitled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeTypeUtils;
import untitled.config.kafka.KafkaProcessor;
import untitled.domain.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMessageVerifier
public class DecreaseStockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        DecreaseStockTest.class
    );

    @Autowired
    private KafkaProcessor processor;

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private InventoryRepository repository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MessageVerifier<Message<?>> messageVerifier;

    private Inventory entity;

    @Before
    public void setup() {
        entity = new Inventory();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test0() {
        //given:

        entity.setId(1L);
        entity.setStock(10);
        entity.setProductName("초코파이");

        repository.save(entity);

        //when:

        OrderPlaced event = new OrderPlaced();

        event.setId(1L);
        event.setProductId("P001");
        event.setUserId("U001");
        event.setProductName("초코파이");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String msg = objectMapper.writeValueAsString(event);

            this.messageVerifier.send(
                    MessageBuilder
                        .withPayload(msg)
                        .setHeader(
                            MessageHeaders.CONTENT_TYPE,
                            MimeTypeUtils.APPLICATION_JSON
                        )
                        .setHeader("type", event.getEventType())
                        .build(),
                    "untitled"
                );

            //then:

            Message<?> receivedMessage =
                this.messageVerifier.receive(
                        "untitled",
                        5000,
                        TimeUnit.MILLISECONDS
                    );

            assertNotNull("Resulted event must be published", receivedMessage);

            StockDecreased outputEvent = objectMapper.readValue(
                (String) receivedMessage.getPayload(),
                StockDecreased.class
            );

            LOGGER.info("Response received: {}", receivedMessage.getPayload());

            assertEquals(outputEvent.getId().longValue(), 1L);
            assertEquals(outputEvent.getStock().intValue(), 9);
            assertEquals(outputEvent.getProductName(), "초코파이");
        } catch (JsonProcessingException e) {
            assertTrue("exception", false);
        }
    }
}
