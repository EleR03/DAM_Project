package org.elide.events.domain.venue;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

/** Entity: venue (location) that contains halls. */
@Entity
public class Venue implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer venueId;
	@NotBlank(message = "Venue name is required!")
	private String name;
	private String address;
	@NotBlank(message = "City is required!")
	private String city;
	private Double latitude;
	private Double longitude;

	public Venue() {
		super();
	}

	public Venue(Integer venueId, String name, String city) {
		super();
		this.venueId = venueId;
		this.name = name;
		this.city = city;
	}

	public Integer getVenueId() { return venueId; }
	public void setVenueId(Integer venueId) { this.venueId = venueId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }
	public Double getLatitude() { return latitude; }
	public void setLatitude(Double latitude) { this.latitude = latitude; }
	public Double getLongitude() { return longitude; }
	public void setLongitude(Double longitude) { this.longitude = longitude; }

	@Override
	public String toString() {
		return "Venue [venueId=" + venueId + ", name=" + name + ", city=" + city + "]";
	}
}
