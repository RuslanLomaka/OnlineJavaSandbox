package com.example.onlinejava.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * The authenticated principal stored in the security context.
 *
 * <p>It is still an {@link OAuth2User} (so existing
 * {@code @AuthenticationPrincipal OAuth2User} parameters keep working) but also
 * carries the {@link AppUser} database id, which features such as discussions
 * use for ownership checks instead of any provider-specific attribute.
 */
public final class AppUserPrincipal implements OAuth2User, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final long userId;

  private final String login;

  private final String displayName;

  private final String avatarUrl;

  private final Map<String, Object> attributes;

  private final List<GrantedAuthority> authorities;

  /**
   * Creates a principal for a persisted user.
   *
   * @param user the persisted user
   * @param attributes raw provider attributes
   * @param authorities granted authorities
   */
  public AppUserPrincipal(
      final AppUser user,
      final Map<String, Object> attributes,
      final Collection<? extends GrantedAuthority> authorities
  ) {
    this.userId = user.getId();
    this.login = user.getLogin();
    this.displayName = user.getDisplayName();
    this.avatarUrl = user.getAvatarUrl();
    // Not Map.copyOf: providers return null for empty profile fields (GitHub's
    // "bio", "company", ...), and Map.copyOf rejects null values.
    this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    this.authorities = List.copyOf(authorities);
  }

  /**
   * Returns the {@link AppUser} database id.
   *
   * @return user id
   */
  public long getUserId() {
    return userId;
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
   * Returns the avatar URL.
   *
   * @return avatar URL, or {@code null}
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getName() {
    return login;
  }
}
