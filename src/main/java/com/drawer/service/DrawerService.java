package com.drawer.service;

import com.drawer.domain.Drawer;

import java.util.List;
import java.util.UUID;

public interface DrawerService {
	UUID create(Drawer d);
	Drawer get(UUID id);
	void updatePartial(UUID id, Drawer patch);
	void delete(UUID id);
	List<Drawer> list(int page, int size);
}
