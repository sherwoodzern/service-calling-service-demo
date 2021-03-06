/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
 *
 */
package greeting;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

@Liveness
@ApplicationScoped
public class GreetLivenessCheck implements HealthCheck {

    private Runtime runtime = Runtime.getRuntime();

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("GreetLivenessCheck")
                .state(runtime.availableProcessors() > 1)
                .withData("availableProcessors", runtime.availableProcessors())
                .build();
    }
}
