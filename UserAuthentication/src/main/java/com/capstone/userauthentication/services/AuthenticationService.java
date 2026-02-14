package com.capstone.userauthentication.services;

import com.capstone.userauthentication.exceptions.IncorrectPasswordException;
import com.capstone.userauthentication.exceptions.UserAlreadyExistsException;
import com.capstone.userauthentication.exceptions.UserNotRegisteredException;
import com.capstone.userauthentication.models.Role;
import com.capstone.userauthentication.models.State;
import com.capstone.userauthentication.models.User;
import com.capstone.userauthentication.repositories.RoleRepository;
import com.capstone.userauthentication.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService implements IAuthenticationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public User signup(String name, String email, String password) throws UserAlreadyExistsException{
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
    public User login(String email, String password) throws UserNotRegisteredException,  IncorrectPasswordException {
        Optional<User> oUser = userRepository.findByEmail(email);
        if (oUser.isEmpty()) throw new UserNotRegisteredException("No User found with this credentials Please Sign Up First");

        User user = oUser.get();
        if(!passwordEncoder.matches(password, user.getPassword())) throw new IncorrectPasswordException("Incorrect Password Please Check");
        return user;
    }
}
