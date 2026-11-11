package com.sabrigur.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** An administrator is a librarian with extra rights (generalization in the class diagram). */
@Entity
@DiscriminatorValue("ADMIN")
@NoArgsConstructor
@SuperBuilder
public class Administrator extends Librarian {

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
