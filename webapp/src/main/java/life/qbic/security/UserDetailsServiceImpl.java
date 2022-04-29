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



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //todo fix me: find user by username and return user details such as name, passwordhash and authorities

        return null;
    }

    private static List<GrantedAuthority> getAuthorities(User testUser) {
        //todo fix me: implement rolemanagement, parse all roles the user has to understhand which rights the user has

        return new ArrayList<>();
    }

}
