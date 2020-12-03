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
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Readiness
@ApplicationScoped
public class GreetReadinessCheck implements HealthCheck {

    @Inject
    UpcaseAccess upcaseAccess;

    @Override
    public HealthCheckResponse call() {
        long start = System.currentTimeMillis();
        upcaseAccess.getUpcase("ping");
        long elapsedTime = System.currentTimeMillis() - start;
        return HealthCheckResponse.named("GreetLivenessCheck")
                .state(elapsedTime < 200)
                .withData("elapsedMS", elapsedTime)
                .build();
    }
}
