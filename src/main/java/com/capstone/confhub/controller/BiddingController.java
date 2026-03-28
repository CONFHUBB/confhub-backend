package com.capstone.confhub.controller;

import com.capstone.confhub.dto.BiddingDTO;
import com.capstone.confhub.dto.response.BiddingResponseDTO;
import com.capstone.confhub.dto.response.BidsSummaryDTO;
import com.capstone.confhub.dto.response.PaperForBiddingDTO;
import com.capstone.confhub.service.BiddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
@Tag(name = "Reviewer Bidding", description = "APIs cho Reviewer đặt bid và xem papers")
public class BiddingController {

    private final BiddingService biddingService;

    @PostMapping
    @Operation(summary = "Submit hoặc update bid cho 1 paper",
            description = "Nếu reviewer đã bid paper này → update. Nếu chưa → tạo mới. Bidding phase phải đang mở.")
    public ResponseEntity<BiddingResponseDTO> submitOrUpdateBid(
            @Valid @RequestBody BiddingDTO dto) {
        return new ResponseEntity<>(biddingService.submitOrUpdateBid(dto), HttpStatus.OK);
    }

    @GetMapping("/papers-for-bidding")
    @Operation(summary = "Lấy danh sách papers cho reviewer bid",
            description = "Trả về papers kèm abstract, subject areas, relevance score. Lọc bỏ conflicting papers.")
    public ResponseEntity<List<PaperForBiddingDTO>> getPapersForBidding(
            @RequestParam Integer reviewerId,
            @RequestParam Integer conferenceId) {
        return ResponseEntity.ok(biddingService.getPapersForBidding(reviewerId, conferenceId));
    }

    @GetMapping("/reviewer/{reviewerId}/conference/{conferenceId}")
    @Operation(summary = "Lấy danh sách bids của reviewer trong 1 conference")
    public ResponseEntity<List<BiddingResponseDTO>> getBidsByReviewerAndConference(
            @PathVariable Integer reviewerId,
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(biddingService.getBidsByReviewerAndConference(reviewerId, conferenceId));
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Lấy danh sách bids cho 1 paper (Chair/Program Chair dùng)")
    public ResponseEntity<List<BiddingResponseDTO>> getBidsByPaper(
            @PathVariable Integer paperId) {
        return ResponseEntity.ok(biddingService.getBidsByPaper(paperId));
    }

    @GetMapping("/summary/{reviewerId}/conference/{conferenceId}")
    @Operation(summary = "Lấy tổng hợp bids (đếm theo loại) cho reviewer trong conference")
    public ResponseEntity<BidsSummaryDTO> getBidsSummary(
            @PathVariable Integer reviewerId,
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(biddingService.getBidsSummary(reviewerId, conferenceId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa 1 bid")
    public ResponseEntity<Void> deleteBid(@PathVariable Integer id) {
        biddingService.deleteBid(id);
        return ResponseEntity.noContent().build();
    }
}
