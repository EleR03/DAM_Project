package org.elide.events.domain.identity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Entity: user account (participant, organizer or administrator). */
@Entity
public class UserAccount implements Serializable, Comparable<UserAccount> {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	@NotNull(message = "UserId is required!")
	private Integer userId;

	@NotBlank(message = "Email is required!")
	@Email(message = "Email format is invalid!")
	private String email;

	@NotBlank(message = "First name is required!")
	private String firstName;

	@NotBlank(message = "Last name is required!")
	private String lastName;

	@Enumerated(EnumType.STRING)
	@NotNull(message = "Role is required!")
	private UserRole role = UserRole.PARTICIPANT;

	@Enumerated(EnumType.STRING)
	@NotNull(message = "Status is required!")
	private AccountStatus status = AccountStatus.ACTIVE;

	public UserAccount() {
		super();
	}

	public UserAccount(Integer userId, String email, String firstName, String lastName, UserRole role) {
		super();
		this.userId = userId;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
	}

	public boolean isActive() {
		return AccountStatus.ACTIVE.equals(status);
	}

	public Integer getUserId() { return userId; }
	public void setUserId(Integer userId) { this.userId = userId; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }
	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }
	public UserRole getRole() { return role; }
	public void setRole(UserRole role) { this.role = role; }
	public AccountStatus getStatus() { return status; }
	public void setStatus(AccountStatus status) { this.status = status; }

	@Override
	public int hashCode() {
		return (userId == null) ? 0 : userId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		UserAccount other = (UserAccount) obj;
		return userId == null ? other.userId == null : userId.equals(other.userId);
	}

	@Override
	public int compareTo(UserAccount other) {
		return this.userId.compareTo(other.userId);
	}

	@Override
	public String toString() {
		return "UserAccount [userId=" + userId + ", email=" + email + ", role=" + role + ", status=" + status + "]";
	}
}
