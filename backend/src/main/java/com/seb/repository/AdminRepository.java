package com.seb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seb.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminRepository extends BaseMapper<Admin> {
}
