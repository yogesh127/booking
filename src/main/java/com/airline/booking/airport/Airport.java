package com.airline.booking.airport;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Airport {

    @Id
    private String code;

    private String name;

    private String city;

    private String country;
}
