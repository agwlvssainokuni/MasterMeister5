/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private ErrorResponseFactory errorResponseFactory;
    @Mock private HttpServletRequest request;

    @Test
    void unhandledExceptionReturnsGenericInternalError() {
        when(request.getLocale()).thenReturn(Locale.JAPANESE);
        var expected = new ErrorResponse("INTERNAL_ERROR", "内部エラーが発生しました。", "cid-1");
        when(errorResponseFactory.create(eq("INTERNAL_ERROR"), eq("errors.internal"), any()))
                .thenReturn(expected);

        var handler = new GlobalExceptionHandler(errorResponseFactory);
        var response = handler.handleUnexpected(new RuntimeException("boom, secret path leak"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(response.getBody().message()).doesNotContain("boom", "secret path leak");
    }
}
