package com.example.onlinejava.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link AppUser}.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  /**
   * Finds a user by their provider identity.
   *
   * @param provider OAuth2 registration id
   * @param providerUserId the provider's stable user id
   * @return the matching user, if any
   */
  Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId);
}
