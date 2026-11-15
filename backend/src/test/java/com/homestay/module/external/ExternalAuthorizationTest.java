package com.homestay.module.external;

import com.homestay.common.exception.BusinessException;
import com.homestay.context.UserContext;
import com.homestay.context.UserContextHolder;
import com.homestay.module.external.controller.ExternalController;
import com.homestay.module.external.service.ExternalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证服务端拒绝非经营者访问。 */
class ExternalAuthorizationTest {
    @AfterEach void clear() { UserContextHolder.clear(); }
    @Test void rejectsGuestAndReceptionistBeforeCallingService() {
        ExternalService service = mock(ExternalService.class);
        ExternalController controller = new ExternalController(service);
        UserContext context = new UserContext(); context.setUserType("employee"); context.setRole("receptionist");
        UserContextHolder.setContext(context);
        assertThrows(BusinessException.class, () -> controller.platforms(true));
        context.setUserType("guest"); context.setRole("owner");
        assertThrows(BusinessException.class, () -> controller.platforms(true));
        verifyNoInteractions(service);
    }
}
