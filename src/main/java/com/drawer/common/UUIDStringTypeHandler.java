package com.drawer.common;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.UUID;

/** CHAR(36) <-> java.util.UUID */
@MappedTypes(UUID.class)
@MappedJdbcTypes(JdbcType.CHAR)
public class UUIDStringTypeHandler extends BaseTypeHandler<UUID> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
		throws SQLException {
		ps.setString(i, parameter.toString());
	}

	@Override
	public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String s = rs.getString(columnName);
		return (s == null) ? null : UUID.fromString(s);
	}

	@Override
	public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String s = rs.getString(columnIndex);
		return (s == null) ? null : UUID.fromString(s);
	}

	@Override
	public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String s = cs.getString(columnIndex);
		return (s == null) ? null : UUID.fromString(s);
	}
}
