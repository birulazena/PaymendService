package com.github.birulazena.PaymentService.integration;

import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.GlobalTotalSumResponseDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.dto.response.UserTotalSumResponseDto;
import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.entity.enums.Status;
import com.github.birulazena.PaymentService.repository.OutboxEventRepository;
import com.github.birulazena.PaymentService.repository.PaymentRepository;
import com.github.birulazena.PaymentService.util.DateTestFactory;
import com.github.birulazena.PaymentService.util.JwtServiceTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentServiceIntegration {

    @Autowired
    private JwtServiceTest jwtServiceTest;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private final BlockingQueue<ConsumerRecord<String, String>> kafkaRecords = new LinkedBlockingQueue<>();

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.9.2")
    );

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(
            DockerImageName.parse("mongo:8.2.4")
    );

    @Container
    static WireMockContainer wireMockContainer = new WireMockContainer("wiremock/wiremock:3.13.1")
            .withMapping("random-number-4", """
                    {
                      "request": {
                        "method": "GET",
                        "urlPath": "/csrng/csrng.php",
                        "queryParameters": {
                          "min": { "equalTo": "1" },
                          "max": { "equalTo": "100" },
                          "count": { "equalTo": "1" }
                        }
                      },
                      "response": {
                        "status": 200,
                        "body": "[4]",
                        "headers": {
                          "Content-Type": "application/json"
                        }
                      }
                    }
                    """);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("app.kafka.topics.payment-events.name", () -> "test-payment-events");
        registry.add("app.kafka.topics.payment-events.partitions", () -> "1");
        registry.add("app.kafka.topics.payment-events.replicas", () -> "1");

        registry.add("spring.mongodb.uri", mongo::getReplicaSetUrl);

        registry.add("external.random-number.url", wireMockContainer::getBaseUrl);
    }

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMockContainer.getHost(), wireMockContainer.getPort());
    }

    @AfterEach
    void tearDown() {
        WireMock.resetToDefault();
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();
        kafkaRecords.clear();
    }

    @KafkaListener(topics = "test-payment-events", groupId = "test-group")
    public void listenPaymentEvents(ConsumerRecord<String, String> record) {
        kafkaRecords.add(record);
    }

    @Test
    void createPaymentSuccessful() throws Exception {
        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        String token = jwtServiceTest.generateAccessToken("Zenya", 1L, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/payments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRequestDto))
                )
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponseDto paymentResponseDto = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                PaymentResponseDto.class
        );

        assertNotNull(paymentResponseDto);
        assertEquals(1L, paymentResponseDto.userId());
        assertEquals(paymentResponseDto.paymentAmount(), paymentRequestDto.paymentAmount());
        assertEquals(paymentResponseDto.orderId(), paymentRequestDto.orderId());
        assertEquals(Status.SUCCESS, paymentResponseDto.status());

        var outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());

        ConsumerRecord<String, String> record = kafkaRecords.poll(5, TimeUnit.SECONDS);

        assertNotNull(record);
    }

    @Test
    void createPaymentExternalNotFoundTest() throws Exception {
        stubFor(get(urlPathEqualTo("/csrng/csrng.php"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        String token = jwtServiceTest.generateAccessToken("Zenya", 1L, "USER");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/payments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRequestDto))
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Random number client not found"))
                .andReturn();

    }

    @Test
    void createPaymentFailedStatus() throws Exception {
        stubFor(get(urlPathEqualTo("/csrng/csrng.php"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[3]")));

        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        String token = jwtServiceTest.generateAccessToken("Zenya", 1L, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/payments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRequestDto))
                )
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponseDto paymentResponseDto = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                PaymentResponseDto.class
        );

        assertEquals(Status.FAILED, paymentResponseDto.status());

        var outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());
    }

    @Test
    void createPaymentsUnauthorizedTest() throws Exception {
        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRequestDto))
                )
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    void getAllByFilterTest() throws Exception {
        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                Instant.parse("2026-02-19T12:12:12Z"), BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                Instant.parse("2026-02-19T12:12:12Z"), BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 1L, Status.SUCCESS,
                Instant.parse("2026-02-19T12:12:12Z"), BigDecimal.valueOf(400)));



        String token = jwtServiceTest.generateAccessToken("Zenya", 1L, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments")
                                .header("Authorization", "Bearer " + token)
                                .param("userId", "1")
                                .param("status", "SUCCESS")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0)) // номер текущей страницы
                .andExpect(jsonPath("$.content[0].orderId").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[1].orderId").value(3))
                .andExpect(jsonPath("$.content[1].status").value("SUCCESS"))
                .andReturn();
    }

    @Test
    void getAllByFilterNotEnoughRights() throws Exception {
        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                null, BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                null, BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 2L, Status.SUCCESS,
                null, BigDecimal.valueOf(400)));



        String token = jwtServiceTest.generateAccessToken("Zenya", 2L, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments")
                                .header("Authorization", "Bearer " + token)
                                .param("userId", "1")
                                .param("status", "SUCCESS")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void getUserTotalSumTest() throws Exception {
        Long userId = 1L;
        Instant from = Instant.parse("2026-02-17T00:00:00Z");
        Instant to = Instant.parse("2026-03-21T23:59:59Z");

        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                null, BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                null, BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 2L, Status.SUCCESS,
                null, BigDecimal.valueOf(400)));

        String token = jwtServiceTest.generateAccessToken("Zenya", userId, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments/stats/users/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .param("from", from.toString())
                                .param("to", to.toString())
                )
                .andExpect(status().isOk())
                .andReturn();

        UserTotalSumResponseDto userTotalSumResponseDto = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                UserTotalSumResponseDto.class
        );

        assertEquals(BigDecimal.valueOf(100), userTotalSumResponseDto.totalSum());
    }

    @Test
    void getUserTotalSumNotEnoughRightTest() throws Exception {
        Long userId = 1L;
        Instant from = Instant.parse("2026-02-17T00:00:00Z");
        Instant to = Instant.parse("2026-03-21T23:59:59Z");

        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                null, BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                null, BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 2L, Status.SUCCESS,
                null, BigDecimal.valueOf(400)));

        String token = jwtServiceTest.generateAccessToken("Zenya", userId + 1, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments/stats/users/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .param("from", from.toString())
                                .param("to", to.toString())
                )
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void getGlobalTotalSumTest() throws Exception {
        Instant from = Instant.parse("2026-02-17T00:00:00Z");
        Instant to = Instant.parse("2026-03-21T23:59:59Z");

        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                null, BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                null, BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 2L, Status.SUCCESS,
                null, BigDecimal.valueOf(400)));

        String token = jwtServiceTest.generateAccessToken("Zenya", 2L, "ADMIN");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments/stats/total")
                                .header("Authorization", "Bearer " + token)
                                .param("from", from.toString())
                                .param("to", to.toString())
                )
                .andExpect(status().isOk())
                .andReturn();

        GlobalTotalSumResponseDto globalTotalSumResponseDto = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                GlobalTotalSumResponseDto.class
        );

        assertEquals(BigDecimal.valueOf(500), globalTotalSumResponseDto.totalSum());
    }

    @Test
    void getGlobalTotalSumNotEnoughRightsTest() throws Exception {
        Instant from = Instant.parse("2026-02-17T00:00:00Z");
        Instant to = Instant.parse("2026-03-21T23:59:59Z");

        Payment payment1 = paymentRepository.save(new Payment(null, 1L, 1L, Status.SUCCESS,
                null, BigDecimal.valueOf(100)));
        Payment payment2 = paymentRepository.save(new Payment(null, 2L, 1L, Status.FAILED,
                null, BigDecimal.valueOf(200)));
        Payment payment3 = paymentRepository.save(new Payment(null, 3L, 2L, Status.SUCCESS,
                null, BigDecimal.valueOf(400)));

        String token = jwtServiceTest.generateAccessToken("Zenya", 2L, "USER");

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/payments/stats/total")
                                .header("Authorization", "Bearer " + token)
                                .param("from", from.toString())
                                .param("to", to.toString())
                )
                .andExpect(status().isForbidden())
                .andReturn();
    }
}
