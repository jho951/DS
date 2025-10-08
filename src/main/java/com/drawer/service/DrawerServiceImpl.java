package com.drawer.service;

import com.drawer.domain.Drawer;
import com.drawer.repository.DrawerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrawerServiceImpl implements DrawerService {

	private final DrawerMapper mapper;

	@Transactional
	@Override
	public UUID create(Drawer d) {
		UUID id = UUID.randomUUID();
		d.setId(id);
		int rows = mapper.insert(d);
		if (rows != 1) {
			throw new IllegalStateException("Insert failed: affected rows=" + rows);
		}
		return id;
	}

	/** 단건 조회(읽기 전용 트랜잭션) */
	@Transactional(readOnly = true)
	@Override
	public Drawer get(UUID id) {
		return mapper.findById(id);
	}

	/** 부분 수정: 존재하지 않으면 예외 */
	@Transactional
	@Override
	public void updatePartial(UUID id, Drawer patch) {
		patch.setId(id);
		int rows = mapper.updatePartial(patch);
		if (rows == 0) {
			throw new IllegalArgumentException("Not found: id=" + id);
		}
	}

	/** 삭제: 존재하지 않으면 예외 */
	@Transactional
	@Override
	public void delete(UUID id) {
		int rows = mapper.delete(id);
		if (rows == 0) {
			throw new IllegalArgumentException("Not found: id=" + id);
		}
	}

	/** 목록 조회(읽기 전용 트랜잭션) */
	@Transactional(readOnly = true)
	@Override
	public List<Drawer> list(int page, int size) {
		int limit = Math.max(1, size);
		int offset = Math.max(0, (page - 1) * limit);
		return mapper.findPage(offset, limit);
	}
}
