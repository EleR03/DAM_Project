package org.elide.events.domain.services;

import org.elide.events.domain.identity.UserAccount;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class UserAccountRepositoryBase extends InMemoryEntityRepositoryBase<UserAccount> implements IUserAccountRepository {
	@Override
	protected Integer idOf(UserAccount entity) {
		return entity.getUserId();
	}

	@Override
	protected void assignId(UserAccount entity, Integer id) {
		entity.setUserId(id);
	}
}
