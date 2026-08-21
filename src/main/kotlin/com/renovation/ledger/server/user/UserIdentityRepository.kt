package com.renovation.ledger.server.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserIdentityRepository : JpaRepository<UserIdentityEntity, String> {
    fun findByProviderAndOpenid(provider: String, openid: String): UserIdentityEntity?
    fun findFirstByUnionid(unionid: String): UserIdentityEntity?
}
