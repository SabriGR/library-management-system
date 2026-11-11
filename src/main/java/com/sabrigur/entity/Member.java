package com.sabrigur.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("MEMBER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Member extends User {

    @Builder.Default
    private LocalDate membershipDate = LocalDate.now();

    @Override
    public String getRole() {
        return "MEMBER";
    }
}
