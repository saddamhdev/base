package snvn.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import snvn.common.dto.AccountResponse;
import snvn.model.User;
import snvn.userservice.client.feign.AccountClient;
import snvn.userservice.client.grpc.AccountGrpcClient;
import snvn.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private final AccountClient accountClient;
    private final AccountGrpcClient accountGrpcClient;
    public UserService(@Lazy AccountClient accountClient, AccountGrpcClient accountGrpcClient) {
        this.accountClient = accountClient;
        this.accountGrpcClient = accountGrpcClient;
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Create a new user
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Update an existing user
     */
    public Optional<User> updateUser(Long id, User userDetails) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (userDetails.getUsername() != null) {
                user.setUsername(userDetails.getUsername());
            }
            if (userDetails.getEmail() != null) {
                user.setEmail(userDetails.getEmail());
            }
            if (userDetails.getPassword() != null) {
                user.setPassword(userDetails.getPassword());
            }
            if (userDetails.getFirstName() != null) {
                user.setFirstName(userDetails.getFirstName());
            }
            if (userDetails.getLastName() != null) {
                user.setLastName(userDetails.getLastName());
            }
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    /**
     * Delete a user
     */
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if user exists by ID
     */
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }


    public AccountResponse getUserAccount(Long userId) {
        return accountClient.getAccount(userId);
    }

    public snvn.grpc.AccountResponse getUserAccountGrpc(Long userId) {
       System.out.println(accountGrpcClient.getAccountByEmail("test@gmail.com"));
        return accountGrpcClient.getAccount(userId);
    }

}

