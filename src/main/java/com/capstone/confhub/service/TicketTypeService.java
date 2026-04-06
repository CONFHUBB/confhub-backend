package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.TicketTypeRequest;
import com.capstone.confhub.dto.response.TicketTypeResponse;

import java.util.List;

public interface TicketTypeService {
    TicketTypeResponse create(Integer conferenceId, TicketTypeRequest request);
    TicketTypeResponse update(Integer id, TicketTypeRequest request);
    void delete(Integer id);
    List<TicketTypeResponse> getByConference(Integer conferenceId, boolean activeOnly);
    List<TicketTypeResponse> getForUser(Integer conferenceId, Integer userId);
}
