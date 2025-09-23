// ContextoFotoDatoRepository.java
package com.example.repository;

import com.example.domain.FotoDato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextoFotoDatoRepository extends JpaRepository<FotoDato, Long> {
}