package com.example.dao;

import com.example.domain.Apu;
import com.example.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MaterialDao extends JpaRepository<Material, Long> {


}
