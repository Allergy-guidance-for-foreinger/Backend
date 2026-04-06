package com.mealguide.mealguide_api.login.domain;

import com.mealguide.mealguide_api.global.config.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_oauth_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOauthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    public static UserOauthAccount createGoogleAccount(User user, String providerUserId, String providerEmail) {
        UserOauthAccount oauthAccount = new UserOauthAccount();
        oauthAccount.user = user;
        oauthAccount.provider = "GOOGLE";
        oauthAccount.providerUserId = providerUserId;
        oauthAccount.providerEmail = providerEmail;
        return oauthAccount;
    }
}
