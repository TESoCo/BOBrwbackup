package com.example.dao;

import com.example.domain.Apu;
import com.example.domain.Material;
import com.example.domain.MaterialesApu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialDao extends JpaRepository<Material, Long> {



}
