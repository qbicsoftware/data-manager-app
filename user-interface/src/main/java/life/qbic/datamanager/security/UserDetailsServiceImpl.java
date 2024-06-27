package life.qbic.datamanager.security;

import java.util.List;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserPassword;
import life.qbic.identity.api.UserPasswordService;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.User;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserInformationService userInformationService;

  private final UserPasswordService userPasswordService;

  private final UserAuthorityProvider userAuthorityProvider;

  @Autowired
  UserDetailsServiceImpl(UserInformationService userInformationService,
      UserPasswordService userPasswordService,
      UserAuthorityProvider userAuthorityProvider) {
    this.userInformationService = userInformationService;
    this.userPasswordService = userPasswordService;
    this.userAuthorityProvider = userAuthorityProvider;
  }

  @Override
  /**
   * In our case the mail address is the username
   */
  public UserDetails loadUserByUsername(String mailAddress) throws UsernameNotFoundException {
    // Then search for a user with the provided mail address
    var userInfo = userInformationService.findByEmail(mailAddress)
        .orElseThrow(() -> new UsernameNotFoundException("Cannot find user"));
    var encryptedPassword = userPasswordService.findEncryptedPasswordForUser(userInfo.id())
        .map(UserPassword::encryptedPassword);
    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(
        userInfo.id());
    var user = new User(userInfo.id(), userInfo.fullName(), userInfo.platformUserName(),
        userInfo.emailAddress(),
        encryptedPassword.orElseGet(null), userInfo.isActive());
    return new QbicUserDetails(user, authorities);
  }
}
