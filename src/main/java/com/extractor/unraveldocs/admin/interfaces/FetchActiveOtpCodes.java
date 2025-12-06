package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.ActiveOtpListData;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;



public interface FetchActiveOtpCodes {
    UnravelDocsResponse<ActiveOtpListData> fetchActiveOtpCodes(int page, int size);
}
