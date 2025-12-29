package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.dto.TwseApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * 台灣證交所 API 客戶端
 */
@Slf4j
@Component
public class TwseApiClient {

    private static final String TWSE_API_URL = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp";
    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwseApiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();

        this.objectMapper = objectMapper;
    }

    /**
     * 查詢台股即時報價
     *
     * @param symbol 股票代號（例如: 2330）
     * @return 股票資料，若查詢失敗則返回 empty
     */
    public Optional<TwseApiResponse.TwseStockData> getStockQuote(String symbol) {
        try {
            String url = String.format("%s?ex_ch=tse_%s.tw", TWSE_API_URL, symbol);

            log.debug("TWSE API request - symbol={}", symbol);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("TWSE API HTTP error - symbol={}, status={}", symbol, response.getStatusCode());
                return Optional.empty();
            }

            TwseApiResponse apiResponse = objectMapper.readValue(response.getBody(), TwseApiResponse.class);

            if (!apiResponse.isSuccess()) {
                log.warn("TWSE API error - symbol={}, code={}, message={}", symbol, apiResponse.getReturnCode(), apiResponse.getReturnMessage());
                return Optional.empty();
            }

            TwseApiResponse.TwseStockData stockData = apiResponse.getStockDataList().get(0);
            log.debug("TWSE API response - name={}, symbol={}, price={}", stockData.getName(), symbol, stockData.getCurrentPrice());

            return Optional.of(stockData);

        } catch (RestClientException e) {
            log.error("TWSE API network error - symbol={}, error={}", symbol, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("TWSE API parse error - symbol={}, error={}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
