package life.qbic.authorization.security;

import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.EmailAddress.EmailValidationException;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authorization.SystemPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  private final SystemPermissionService systemPermissionService;

  @Autowired
  UserDetailsServiceImpl(UserRepository userRepository,
                         SystemPermissionService systemPermissionService) {
    this.userRepository = userRepository;
    this.systemPermissionService = systemPermissionService;
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
    List<GrantedAuthority> authorities = systemPermissionService.loadUserPermissions(user.id());
    return new QbicUserDetails(user, authorities);
  }
}
