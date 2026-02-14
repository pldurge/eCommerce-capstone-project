package com.capstone.userauthentication.services;

import com.capstone.userauthentication.exceptions.IncorrectPasswordException;
import com.capstone.userauthentication.exceptions.UserAlreadyExistsException;
import com.capstone.userauthentication.exceptions.UserNotRegisteredException;
import com.capstone.userauthentication.models.Role;
import com.capstone.userauthentication.models.Session;
import com.capstone.userauthentication.models.State;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.pojos.UserToken;
import com.capstone.userauthentication.repositories.RoleRepository;
import com.capstone.userauthentication.repositories.SessionRepository;
import com.capstone.userauthentication.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class AuthenticationService implements IAuthenticationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecretKey secretKey;
    private final SessionRepository sessionRepository;

    public AuthenticationService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, SecretKey secretKey, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.secretKey = secretKey;
        this.sessionRepository = sessionRepository;
    }


    @Override
    public User signup(String name, String email, String password) {
        Optional<User> oUser =  userRepository.findByEmail(email);
        if (oUser.isPresent()) throw new UserAlreadyExistsException("User already exists Please Login to Continue Or Sign Up with different credentials");

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(new Date());
        user.setLastUpdatedAt(new Date());
        user.setState(State.ACTIVE);

        Role role;
        Optional<Role> optionalRole = roleRepository.findByName("DEFAULT");

        if(optionalRole.isEmpty()){
            role = new Role();
            role.setName("DEFAULT");
            role.setCreatedAt(new Date());
            role.setLastUpdatedAt(new Date());
            role.setState(State.ACTIVE);
            roleRepository.save(role);
        }else{
            role = optionalRole.get();
        }

        List<Role> roles = new ArrayList<>();
        roles.add(role);

        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public UserToken login(String email, String password) {
        Optional<User> oUser = userRepository.findByEmail(email);
        if (oUser.isEmpty())
            throw new UserNotRegisteredException("No User found with this credentials Please Sign Up First");

        User user = oUser.get();
        if(!passwordEncoder.matches(password, user.getPassword()))
            throw new IncorrectPasswordException("Incorrect Password Please Check");

        Map<String, Object> payload = new HashMap<>();
        long nowInMillis = System.currentTimeMillis(); //returns time in epoch
        payload.put("iat", nowInMillis); //issued at
        payload.put("exp", nowInMillis + 100000000);
        payload.put("userId", user.getId());
        payload.put("iss", "scaler"); // issuer
        payload.put("scope", user.getRoles());

        String token = Jwts.builder().claims(payload)
                .signWith(secretKey)
                .compact();

        Session session = new Session();
        session.setToken(token);
        session.setUser(user);
        session.setState(State.ACTIVE);
        sessionRepository.save(session);

        return new UserToken(token, user);
    }

    @Override
    public boolean validateToken(String token) {
        Optional<Session> oSession = sessionRepository.findByToken(token);
        if(oSession.isEmpty()) return false;

        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey).build();
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        long expiryTime = (long) claims.get("exp");
        long nowInMillis = System.currentTimeMillis();

        if(nowInMillis > expiryTime){
            Session session = oSession.get();
            session.setState(State.INACTIVE);
            sessionRepository.save(session);
            return false;
        }
        return true;
    }
}
