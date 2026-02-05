package com.controlphonedesk.rbac.repo;

import com.controlphonedesk.rbac.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    java.util.List<Role> findAll();

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findWithPermissionsById(Long id);
}
