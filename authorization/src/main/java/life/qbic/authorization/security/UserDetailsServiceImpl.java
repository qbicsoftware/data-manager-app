package life.qbic.authorization.security;

import java.util.List;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.EmailAddress.EmailValidationException;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  private final UserAuthorityProvider userAuthorityProvider;

  @Autowired
  UserDetailsServiceImpl(UserRepository userRepository,
      UserAuthorityProvider userAuthorityProvider) {
    this.userRepository = userRepository;
    this.userAuthorityProvider = userAuthorityProvider;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    EmailAddress email;
    // Check if the mail address is valid
    try {
      email = EmailAddress.from(username);
    } catch (EmailValidationException e) {
      throw new UsernameNotFoundException("Cannot find user");
    }
    // Then search for a user with the provided mail address
    var user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Cannot find user"));
    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(user.id());
    return new QbicUserDetails(user, authorities);
  }
}
