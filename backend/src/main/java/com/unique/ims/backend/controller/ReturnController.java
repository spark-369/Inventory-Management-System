package com.unique.ims.backend.controller;

import com.unique.ims.backend.dto.ReturnDto;
import com.unique.ims.backend.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping
    public ResponseEntity<ReturnDto> createReturn(@Valid @RequestBody ReturnDto returnDto) {
        return ResponseEntity.ok(returnService.createReturn(returnDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReturnDto> updateReturn(@PathVariable Long id, @Valid @RequestBody ReturnDto returnDto) {
        return ResponseEntity.ok(returnService.updateReturn(id, returnDto));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReturnDto> approveReturn(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.approveReturn(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnDto> getReturnById(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.getReturnById(id));
    }

    @GetMapping("/number/{returnNumber}")
    public ResponseEntity<ReturnDto> getReturnByNumber(@PathVariable String returnNumber) {
        return ResponseEntity.ok(returnService.getReturnByNumber(returnNumber));
    }

    @GetMapping
    public ResponseEntity<List<ReturnDto>> getAllReturns() {
        return ResponseEntity.ok(returnService.getAllReturns());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReturn(@PathVariable Long id) {
        returnService.deleteReturn(id);
        return ResponseEntity.noContent().build();
    }
}
