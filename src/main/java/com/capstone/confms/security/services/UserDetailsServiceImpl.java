package com.capstone.confms.security.services;

import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserRole;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());

        return UserDetailsImpl.build(user, userRoles);
    }
}
