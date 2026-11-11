package com.sabrigur.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("LIBRARIAN")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Librarian extends User {

    private String employeeNo;

    @Override
    public String getRole() {
        return "LIBRARIAN";
    }
}
