package com.seb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seb.entity.Website;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WebsiteRepository extends BaseMapper<Website> {
}
