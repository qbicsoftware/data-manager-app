package life.qbic.datamanager.security;

import java.util.List;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.User;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import life.qbic.user.api.UserInfo;
import life.qbic.user.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserInformationService userInformationService;

  private final UserAuthorityProvider userAuthorityProvider;

  @Autowired
  UserDetailsServiceImpl(UserInformationService userInformationService,
      UserAuthorityProvider userAuthorityProvider) {
    this.userInformationService = userInformationService;
    this.userAuthorityProvider = userAuthorityProvider;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // Then search for a user with the provided mail address
    var userInfo = userInformationService.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("Cannot find user"));
    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(
        userInfo.id());
    var user = new User(userInfo.id(), userInfo.fullName(), userInfo.emailAddress(),
        userInfo.encryptedPassword(), userInfo.isActive());
    return new QbicUserDetails(user, authorities);
  }

  private User convert(UserInfo userInfo) {
    return new User(userInfo.id(), userInfo.fullName(), userInfo.emailAddress(),
        userInfo.encryptedPassword(),
        userInfo.isActive());
  }
}
