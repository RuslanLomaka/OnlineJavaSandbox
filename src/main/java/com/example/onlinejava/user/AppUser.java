package com.example.onlinejava.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A person who has signed in to the application through an OAuth2 provider.
 *
 * <p>The natural key is {@code (provider, providerUserId)}; {@code login},
 * {@code displayName} and {@code avatarUrl} are a snapshot of the provider's
 * profile, refreshed on every login.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private String provider;

  @Column(name = "provider_user_id", nullable = false, updatable = false)
  private String providerUserId;

  @Column(nullable = false)
  private String login;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_login_at", nullable = false)
  private Instant lastLoginAt;

  /**
   * Required by JPA.
   */
  protected AppUser() {
  }

  /**
   * Creates a new, not yet persisted user.
   *
   * @param provider OAuth2 registration id, e.g. {@code github}
   * @param providerUserId the provider's stable id for this user
   * @param login the provider's username/handle
   * @param displayName human-readable name
   * @param avatarUrl avatar image URL, may be {@code null}
   * @param now creation time, also used as the first login time
   */
  public AppUser(
      final String provider,
      final String providerUserId,
      final String login,
      final String displayName,
      final String avatarUrl,
      final Instant now
  ) {
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.login = login;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.createdAt = now;
    this.lastLoginAt = now;
  }

  /**
   * Refreshes the profile snapshot and records a new login.
   *
   * @param newLogin current username/handle
   * @param newDisplayName current display name
   * @param newAvatarUrl current avatar URL, may be {@code null}
   * @param now login time
   */
  public void recordLogin(
      final String newLogin,
      final String newDisplayName,
      final String newAvatarUrl,
      final Instant now
  ) {
    this.login = newLogin;
    this.displayName = newDisplayName;
    this.avatarUrl = newAvatarUrl;
    this.lastLoginAt = now;
  }

  /**
   * Returns the database id.
   *
   * @return id, {@code null} before the entity is persisted
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the OAuth2 provider (registration id).
   *
   * @return provider, e.g. {@code github}
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Returns the provider's stable id for this user.
   *
   * @return provider user id
   */
  public String getProviderUserId() {
    return providerUserId;
  }

  /**
   * Returns the username/handle.
   *
   * @return login
   */
  public String getLogin() {
    return login;
  }

  /**
   * Returns the human-readable name.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the avatar image URL.
   *
   * @return avatar URL, or {@code null}
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }

  /**
   * Returns when the user first signed in.
   *
   * @return creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns when the user last signed in.
   *
   * @return last login time
   */
  public Instant getLastLoginAt() {
    return lastLoginAt;
  }
}
