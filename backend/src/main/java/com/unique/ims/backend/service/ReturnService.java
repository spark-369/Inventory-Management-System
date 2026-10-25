package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.ReturnDto;
import java.util.List;

public interface ReturnService {
    ReturnDto createReturn(ReturnDto returnDto);
    ReturnDto updateReturn(Long id, ReturnDto returnDto);
    ReturnDto getReturnById(Long id);
    ReturnDto getReturnByNumber(String returnNumber);
    List<ReturnDto> getAllReturns();
    void deleteReturn(Long id);
    ReturnDto approveReturn(Long id);
}