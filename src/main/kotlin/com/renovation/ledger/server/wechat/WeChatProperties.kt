package com.renovation.ledger.server.wechat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.wechat")
class WeChatProperties {
    var mpAppId: String = ""
    var mpSecret: String = ""
    var appAppId: String = ""
    var appSecret: String = ""
}
