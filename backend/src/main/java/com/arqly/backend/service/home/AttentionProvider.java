package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import java.util.List;

public interface AttentionProvider {
    List<AttentionItemResponse> provide(HomeUserContext context);
}
