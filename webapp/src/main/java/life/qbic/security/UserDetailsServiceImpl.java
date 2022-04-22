package life.qbic.security;

import java.util.ArrayList;
import java.util.List;
import life.qbic.usermanagement.User;
import life.qbic.usermanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    //@Autowired
    //private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //User testUser = userRepository.findByUsername(username);
        //if (testUser == null) {
        //    throw new UsernameNotFoundException("No testUser present with username: " + username);
        //} else {
            //todo fix me
            //return new org.springframework.security.core.userdetails.User(testUser.getUsername(), testUser.getHashedPassword(),
            //        getAuthorities(testUser));
            return null;
        //}
    }

    private static List<GrantedAuthority> getAuthorities(User testUser) {
        //todo fix me
        //return testUser.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
        //       .collect(Collectors.toList());
        return new ArrayList<>();
    }

}
