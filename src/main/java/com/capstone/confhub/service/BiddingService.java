package com.capstone.confhub.service;

import com.capstone.confhub.dto.BiddingDTO;
import com.capstone.confhub.dto.response.BiddingResponseDTO;
import com.capstone.confhub.dto.response.BidsSummaryDTO;
import com.capstone.confhub.dto.response.PaperForBiddingDTO;

import java.util.List;

public interface BiddingService {

    /**
     * Submit hoặc update bid cho 1 paper.
     * Nếu reviewer đã bid paper này rồi → update bid value.
     * Nếu chưa → tạo bid mới.
     */
    BiddingResponseDTO submitOrUpdateBid(BiddingDTO dto);

    /**
     * Lấy danh sách bids của reviewer trong 1 conference.
     */
    List<BiddingResponseDTO> getBidsByReviewerAndConference(Integer reviewerId, Integer conferenceId);

    /**
     * Lấy danh sách bids cho 1 paper (Chair dùng).
     */
    List<BiddingResponseDTO> getBidsByPaper(Integer paperId);

    /**
     * Lấy tổng hợp bids (đếm theo loại) cho reviewer trong conference.
     */
    BidsSummaryDTO getBidsSummary(Integer reviewerId, Integer conferenceId);

    /**
     * Xóa bid.
     */
    void deleteBid(Integer bidId);

    /**
     * Lấy danh sách papers cho reviewer bid:
     * - Lọc: papers có conflict với reviewer.
     * - Kèm: abstract, subject areas, relevance score, current bid.
     */
    List<PaperForBiddingDTO> getPapersForBidding(Integer reviewerId, Integer conferenceId);
}
