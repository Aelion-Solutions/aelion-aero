package com.aelion.aero.common.api;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aelion.aero.common.config.AeroConfig;
import org.junit.jupiter.api.Test;

class PanelHttpTest {

    @Test
    void panelClientsShareTheSameOkHttpClient() {
        PanelHttp http = new PanelHttp(false);
        try {
            AeroConfig config = AeroConfig.empty();
            HttpPanelClient first = http.panelClient(config);
            HttpPanelClient second = http.panelClient(config);
            assertSame(http.client(), first.httpClient());
            assertSame(first.httpClient(), second.httpClient());
        } finally {
            http.close();
        }
    }
}
