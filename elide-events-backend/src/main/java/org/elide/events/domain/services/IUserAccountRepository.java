package org.elide.events.domain.services;

import java.util.List;

import javax.persistence.LockModeType;

import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserAccountRepository extends IEntityRepository<UserAccount> {
	List<UserAccount> findByEmail(String email);

	List<UserAccount> findByRole(UserRole role);

	/** Same as getByIdOrNull, but the row is locked (SELECT ... FOR UPDATE) until the end of the running transaction. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from UserAccount u where u.userId = :id")
	UserAccount getByIdForUpdate(@Param("id") Integer id);
}
