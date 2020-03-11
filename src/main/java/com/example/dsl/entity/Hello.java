package com.example.dsl.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Hello {
    @Id
    @GeneratedValue
    private Long id;

}
