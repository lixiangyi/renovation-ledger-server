package com.renovation.ledger.server.wechat

import com.renovation.ledger.server.error.ApiException
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(WeChatProperties::class)
class WeChatClientConfig {

    @Bean
    @ConditionalOnMissingBean
    fun weChatClient(properties: WeChatProperties): WeChatClient {
        return HttpWeChatClient(properties, RestClient.create())
    }
}

class HttpWeChatClient(
    private val properties: WeChatProperties,
    private val rest: RestClient,
) : WeChatClient {
    override fun code2Session(code: String, client: String): WeChatSession {
        if (client == "app") {
            val url =
                "https://api.weixin.qq.com/sns/oauth2/access_token?appid=${properties.appAppId}&secret=${properties.appSecret}&code=$code&grant_type=authorization_code"
            val body = rest.get().uri(url).retrieve().body(WeChatTokenResponse::class.java)
                ?: throw IllegalStateException("empty wechat app session")
            return WeChatSession(openid = body.openid ?: error("no openid"), unionid = body.unionid)
        }
        val url =
            "https://api.weixin.qq.com/sns/jscode2session?appid=${properties.mpAppId}&secret=${properties.mpSecret}&js_code=$code&grant_type=authorization_code"
        val body = rest.get().uri(url).retrieve().body(WeChatTokenResponse::class.java)
            ?: throw IllegalStateException("empty wechat mp session")
        return WeChatSession(openid = body.openid ?: error("no openid"), unionid = body.unionid)
    }

    override fun phoneFromCode(phoneCode: String, client: String): String {
        if (client == "app") {
            throw ApiException(400, "BAD_REQUEST", "请在微信小程序中绑定手机号")
        }
        val tokenUrl =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=${properties.mpAppId}&secret=${properties.mpSecret}"
        val token = rest.get().uri(tokenUrl).retrieve().body(WeChatTokenResponse::class.java)
            ?: throw ApiException(400, "BAD_REQUEST", "无法获取微信 access_token")
        val access = token.access_token ?: throw ApiException(400, "BAD_REQUEST", "无法获取微信 access_token")
        val phoneUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=$access"
        val body = rest.post().uri(phoneUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("code" to phoneCode))
            .retrieve()
            .body(WeChatPhoneResponse::class.java)
            ?: throw ApiException(400, "BAD_REQUEST", "无法解析微信手机号")
        val phone = body.phone_info?.purePhoneNumber ?: body.phone_info?.phoneNumber
        if (phone.isNullOrBlank()) {
            throw ApiException(400, "BAD_REQUEST", "无法解析微信手机号")
        }
        return phone
    }
}

private class WeChatTokenResponse {
    var openid: String? = null
    var unionid: String? = null
    var access_token: String? = null
    var errcode: Int? = null
    var errmsg: String? = null
}

private class WeChatPhoneInfo {
    var phoneNumber: String? = null
    var purePhoneNumber: String? = null
}

private class WeChatPhoneResponse {
    var phone_info: WeChatPhoneInfo? = null
    var errcode: Int? = null
    var errmsg: String? = null
}
