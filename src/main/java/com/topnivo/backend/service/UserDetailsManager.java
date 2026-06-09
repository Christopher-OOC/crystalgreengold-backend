package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserDetailsManager implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username);
        if (Objects.isNull(member)) {
            throw new UsernameNotFoundException(ErrorMessages.NO_SUCH_MEMBER);
        }

        List<SimpleGrantedAuthority> authorities = member
                .getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();

        return new User(
                member.getUsername(),
                member.getPassword(),
                member.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}
