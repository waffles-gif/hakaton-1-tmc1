package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.exception.BadRequestException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.model.Roles;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return UserResponse.from(userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado")));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, String role, String actingEmail) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
        if (!Roles.isValid(role)) {
            throw new BadRequestException("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }
        if (target.getEmail().equals(actingEmail)) {
            throw new BadRequestException("Un administrador no puede cambiar su propio rol");
        }
        target.setRole(role);
        return UserResponse.from(userRepository.save(target));
    }
}
