package com.renovation.ledger.server.auth

import com.renovation.ledger.server.config.JwtService
import com.renovation.ledger.server.error.ApiException
import com.renovation.ledger.server.user.UserEntity
import com.renovation.ledger.server.user.UserIdentityEntity
import com.renovation.ledger.server.user.UserIdentityRepository
import com.renovation.ledger.server.user.UserRepository
import com.renovation.ledger.server.wechat.WeChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class AuthService(
    private val weChatClient: WeChatClient,
    private val users: UserRepository,
    private val identities: UserIdentityRepository,
    private val jwtService: JwtService,
    private val smsCodeStore: SmsCodeStore,
    private val avatarStorage: AvatarStorageService,
    @Value("\${app.dev-login-enabled:false}") private val devLoginEnabled: Boolean,
    @Value("\${app.sms.return-code-in-response:false}") private val returnCodeInResponse: Boolean,
    @Value("\${app.sms.ttl-seconds:300}") private val smsTtlSeconds: Long,
) {
    @Transactional
    fun loginWeChat(request: WeChatLoginRequest): AuthResponse {
        val session = weChatClient.code2Session(request.code, request.client)
        val provider = if (request.client == "app") "wechat_app" else "wechat_mp"
        val existing = identities.findByProviderAndOpenid(provider, session.openid)
            ?: session.unionid?.let { identities.findFirstByUnionid(it) }
        val user = if (existing != null) {
            if (identities.findByProviderAndOpenid(provider, session.openid) == null) {
                identities.save(
                    UserIdentityEntity(
                        userId = existing.userId,
                        provider = provider,
                        openid = session.openid,
                        unionid = session.unionid,
                    ),
                )
            }
            users.findById(existing.userId).orElseThrow()
        } else {
            val created = users.save(UserEntity(nickname = DefaultNickname.fromSuffix(session.openid)))
            identities.save(
                UserIdentityEntity(
                    userId = created.id,
                    provider = provider,
                    openid = session.openid,
                    unionid = session.unionid,
                ),
            )
            created
        }
        return toAuth(user)
    }

    @Transactional
    fun bindPhone(request: BindPhoneRequest): AuthResponse {
        val userId = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApiException(401, "UNAUTHENTICATED", "请重新登录")
        val phone = weChatClient.phoneFromCode(request.phoneCode, request.client)
        val user = users.findById(userId).orElseThrow()
        if (user.phone == phone) {
            return toAuth(user)
        }
        val occupied = users.findByPhone(phone)
        if (occupied != null && occupied.id != user.id) {
            throw ApiException(409, "CONFLICT", "该手机号已绑定其他账号")
        }
        user.phone = phone
        users.save(user)
        return toAuth(user)
    }

    fun sendSms(request: SmsSendRequest): SmsSendResponse {
        val phone = normalizePhone(request.phone)
        if (!phone.matches(Regex("^1\\d{10}$"))) {
            throw ApiException(400, "BAD_REQUEST", "手机号格式不正确")
        }
        if (!returnCodeInResponse) {
            throw ApiException(501, "NOT_IMPLEMENTED", "正式环境短信未开通")
        }
        val code = (100000..999999).random().toString()
        smsCodeStore.put(phone, code, smsTtlSeconds)
        return SmsSendResponse(
            expiresInSec = smsTtlSeconds,
            code = code,
        )
    }

    @Transactional
    fun loginSms(request: SmsLoginRequest): AuthResponse {
        val phone = normalizePhone(request.phone)
        if (!smsCodeStore.consume(phone, request.code.trim())) {
            throw ApiException(400, "BAD_REQUEST", "验证码错误或已过期")
        }
        val existing = users.findByPhone(phone)
        val user = existing ?: users.save(
            UserEntity(
                nickname = DefaultNickname.fromSuffix(phone),
                phone = phone,
            ),
        )
        return toAuth(user)
    }

    @Transactional
    fun devLogin(label: String): AuthResponse {
        if (!devLoginEnabled) {
            throw ApiException(403, "FORBIDDEN", "开发登录未开启")
        }
        val provider = "dev"
        val openid = "dev_$label"
        val existing = identities.findByProviderAndOpenid(provider, openid)
        val user = if (existing != null) {
            users.findById(existing.userId).orElseThrow()
        } else {
            val created = users.save(UserEntity(nickname = label))
            identities.save(
                UserIdentityEntity(
                    userId = created.id,
                    provider = provider,
                    openid = openid,
                ),
            )
            created
        }
        return toAuth(user)
    }

    fun getMe(): MeResponse = toMe(currentUser())

    @Transactional
    fun updateMe(request: UpdateMeRequest): MeResponse {
        val user = currentUser()
        user.nickname = request.nickname.trim().ifBlank { "我" }
        users.save(user)
        return toMe(user)
    }

    @Transactional
    fun updateAvatar(file: MultipartFile): MeResponse {
        val user = currentUser()
        val nextUrl = avatarStorage.save(file)
        val oldUrl = user.avatarUrl
        user.avatarUrl = nextUrl
        users.save(user)
        if (oldUrl != null && oldUrl != nextUrl) {
            avatarStorage.deleteByUrl(oldUrl)
        }
        return toMe(user)
    }

    @Transactional
    fun clearAvatar(): MeResponse {
        val user = currentUser()
        val oldUrl = user.avatarUrl
        user.avatarUrl = null
        users.save(user)
        avatarStorage.deleteByUrl(oldUrl)
        return toMe(user)
    }

    private fun currentUser(): UserEntity {
        val userId = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApiException(401, "UNAUTHENTICATED", "请重新登录")
        return users.findById(userId).orElseThrow {
            ApiException(401, "UNAUTHENTICATED", "请重新登录")
        }
    }

    private fun normalizePhone(raw: String): String = raw.trim()

    private fun toAuth(user: UserEntity): AuthResponse =
        AuthResponse(
            userId = user.id,
            token = jwtService.create(user.id),
            nickname = user.nickname,
            phone = user.phone,
            avatarUrl = user.avatarUrl,
        )

    private fun toMe(user: UserEntity): MeResponse =
        MeResponse(
            userId = user.id,
            nickname = user.nickname,
            phone = user.phone,
            avatarUrl = user.avatarUrl,
        )
}
