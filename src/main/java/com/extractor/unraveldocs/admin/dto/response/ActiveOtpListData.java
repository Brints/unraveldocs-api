package com.extractor.unraveldocs.admin.dto.response;

import com.extractor.unraveldocs.admin.dto.ActiveOtpData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveOtpListData {
    private List<ActiveOtpData> otps;
    private long totalOtps;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
