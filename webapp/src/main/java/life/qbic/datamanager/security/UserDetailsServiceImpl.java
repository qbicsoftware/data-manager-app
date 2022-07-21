package life.qbic.datamanager.security;

import java.util.ArrayList;
import java.util.List;
import life.qbic.identity.domain.user.EmailAddress;
import life.qbic.identity.domain.user.EmailAddress.EmailValidationException;
import life.qbic.identity.domain.user.User;
import life.qbic.identity.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Autowired
  UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    EmailAddress email;
    // Check if the email address is valid
    try {
      email = EmailAddress.from(username);
    } catch (EmailValidationException e) {
      throw new UsernameNotFoundException("Cannot find user");
    }
    // Then search for a user with the provided email address
    var user = userRepository.findByEmail(email);
    return new QbicUserDetails(
        user.orElseThrow(() -> new UsernameNotFoundException("Cannot find user")));
  }

  private static List<GrantedAuthority> getAuthorities(User testUser) {
    // todo fix me: implement rolemanagement, parse all roles the user has to understhand which
    // rights the user has

    return new ArrayList<>();
  }
}
