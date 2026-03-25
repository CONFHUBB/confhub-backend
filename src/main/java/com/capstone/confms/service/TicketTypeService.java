package com.capstone.confms.service;

import com.capstone.confms.dto.request.TicketTypeRequest;
import com.capstone.confms.dto.response.TicketTypeResponse;

import java.util.List;

public interface TicketTypeService {
    TicketTypeResponse create(Integer conferenceId, TicketTypeRequest request);
    TicketTypeResponse update(Integer id, TicketTypeRequest request);
    void delete(Integer id);
    List<TicketTypeResponse> getByConference(Integer conferenceId, boolean activeOnly);
}
