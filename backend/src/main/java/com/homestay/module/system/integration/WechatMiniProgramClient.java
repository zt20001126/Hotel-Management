package com.homestay.module.system.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.homestay.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
public class WechatMiniProgramClient {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300;

    @Value("${wechat.mini-program.app-id:${WECHAT_MINI_PROGRAM_APP_ID:}}")
    private String appId;

    @Value("${wechat.mini-program.app-secret:${WECHAT_MINI_PROGRAM_APP_SECRET:}}")
    private String appSecret;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String cachedAccessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public WechatMiniProgramClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getOpenid(String loginCode) {
        requireCredentials();
        if (!StringUtils.hasText(loginCode)) {
            throw new BusinessException("微信登录凭证不能为空");
        }

        URI uri = UriComponentsBuilder.fromUriString(LOGIN_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", loginCode)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUri();

        JsonNode response = getJson(uri);
        checkWechatError(response);
        String openid = response.path("openid").asText();
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException("微信登录验证失败");
        }
        return openid;
    }

    public String getPhoneNumber(String phoneCode) {
        requireCredentials();
        if (!StringUtils.hasText(phoneCode)) {
            throw new BusinessException("手机号授权凭证不能为空");
        }

        URI uri = UriComponentsBuilder.fromUriString(PHONE_NUMBER_URL)
                .queryParam("access_token", getAccessToken())
                .build()
                .encode()
                .toUri();

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of("code", phoneCode));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("构造微信手机号请求失败");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(requestBody.getBytes(StandardCharsets.UTF_8).length);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        String responseBody;
        try {
            responseBody = restTemplate.postForObject(uri, request, String.class);
        } catch (RestClientResponseException exception) {
            throw new BusinessException("微信手机号接口请求失败（HTTP "
                    + exception.getStatusCode().value() + "），请稍后重试");
        } catch (RestClientException exception) {
            throw new BusinessException("获取微信手机号失败，请稍后重试");
        }
        JsonNode response = parseJson(responseBody);
        checkWechatError(response);

        String phoneNumber = response == null
                ? ""
                : response.path("phone_info").path("phoneNumber").asText();
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BusinessException("微信未返回有效手机号");
        }
        return phoneNumber;
    }

    private synchronized String getAccessToken() {
        if (StringUtils.hasText(cachedAccessToken)
                && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
            return cachedAccessToken;
        }

        URI uri = UriComponentsBuilder.fromUriString(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .build()
                .encode()
                .toUri();
        JsonNode response = getJson(uri);
        checkWechatError(response);

        String token = response.path("access_token").asText();
        long expiresIn = response.path("expires_in").asLong(7200);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("获取微信接口凭证失败");
        }
        cachedAccessToken = token;
        accessTokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        return token;
    }

    private JsonNode getJson(URI uri) {
        String responseBody;
        try {
            responseBody = restTemplate.getForObject(uri, String.class);
        } catch (RestClientException exception) {
            throw new BusinessException("连接微信服务失败，请稍后重试");
        }
        return parseJson(responseBody);
    }

    private JsonNode parseJson(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException("微信服务未返回有效结果");
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("微信服务返回了无法识别的响应");
        }
    }

    private void requireCredentials() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BusinessException("服务端尚未配置微信小程序凭证");
        }
    }

    private void checkWechatError(JsonNode response) {
        if (response == null) {
            throw new BusinessException("微信服务未返回有效结果");
        }
        JsonNode errorCode = response.get("errcode");
        if (errorCode != null && errorCode.asInt() != 0) {
            throw new BusinessException("微信凭证无效或已过期，请重新授权");
        }
    }
}
