package com.gastos.controller;

import com.gastos.dto.person.PersonResponse;
import com.gastos.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonRepository personRepository;

    @GetMapping
    public ResponseEntity<List<PersonResponse>> list() {
        List<PersonResponse> persons = personRepository.findAll().stream()
                .map(p -> new PersonResponse(p.getId(), p.getName(), p.getColor()))
                .toList();
        return ResponseEntity.ok(persons);
    }
}
