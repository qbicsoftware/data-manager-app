package life.qbic.security;

import java.util.List;
import java.util.stream.Collectors;
import life.qbic.data.entity.TestUser;
import life.qbic.data.service.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TestUser testUser = userRepository.findByUsername(username);
        if (testUser == null) {
            throw new UsernameNotFoundException("No testUser present with username: " + username);
        } else {
            return new org.springframework.security.core.userdetails.User(testUser.getUsername(), testUser.getHashedPassword(),
                    getAuthorities(testUser));
        }
    }

    private static List<GrantedAuthority> getAuthorities(TestUser testUser) {
        return testUser.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toList());

    }

}
