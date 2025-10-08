package com.drawer.repository;

import com.drawer.domain.Drawer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DrawerMapper {
	int insert(Drawer drawer);
	Drawer findById(@Param("id") UUID id);
	int updatePartial(Drawer drawer);
	int delete(@Param("id") UUID id);
	List<Drawer> findPage(@Param("offset") int offset, @Param("limit") int limit);
}
